package com.peanutsmin.octlabeling;

import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.function.Supplier;

public class CanvasInteractionController {
    private final ImageCanvas canvas;
    private final AnnotationStore store;
    private final Supplier<File> currentImageFile;
    private final Supplier<LabelClass> selectedLabel;
    private final Supplier<AnnotationMode> selectedMode;
    private final Function<LabelClass, String> labelDisplay;
    private final Runnable onAnnotationsChanged;

    private Rectangle currentRect;
    private Polygon currentMask;
    private ArrayList<Double> currentMaskPoints = new ArrayList<>();
    private DragMode dragMode = DragMode.NONE;
    private int selectedIndex = -1;
    private int selectedMaskIndex = -1;
    private double startX, startY;
    private double dragStartX, dragStartY;
    private double startRectX, startRectY, startRectW, startRectH;

    private static final double MIN_BOX_SIZE = 5.0;
    private static final int MIN_MASK_POINTS = 3;
    private static final double MIN_MASK_POINT_DISTANCE = 4.0;
    private static final double RESIZE_HANDLE_SIZE = 8.0;

    private enum DragMode {
        NONE,
        DRAW,
        MOVE,
        RESIZE_N,
        RESIZE_E,
        RESIZE_S,
        RESIZE_W,
        RESIZE_NE,
        RESIZE_NW,
        RESIZE_SE,
        RESIZE_SW,
        MASK
    }

    public CanvasInteractionController(
            ImageCanvas canvas,
            AnnotationStore store,
            Supplier<File> currentImageFile,
            Supplier<LabelClass> selectedLabel,
            Supplier<AnnotationMode> selectedMode,
            Function<LabelClass, String> labelDisplay,
            Runnable onAnnotationsChanged
    ) {
        this.canvas = canvas;
        this.store = store;
        this.currentImageFile = currentImageFile;
        this.selectedLabel = selectedLabel;
        this.selectedMode = selectedMode;
        this.labelDisplay = labelDisplay;
        this.onAnnotationsChanged = onAnnotationsChanged;
    }

    public void install() {
        canvas.setOnMousePressed(this::handlePressed);
        canvas.setOnMouseDragged(this::handleDragged);
        canvas.setOnMouseReleased(this::handleReleased);
        canvas.setOnMouseMoved(this::handleMoved);
    }

    public void resetSelection() {
        selectedIndex = -1;
        selectedMaskIndex = -1;
        dragMode = DragMode.NONE;
        currentRect = null;
        currentMask = null;
        currentMaskPoints.clear();
        canvas.clearSelection();
    }

    public boolean applySelectedLabel(LabelClass label) {
        if (selectedMaskIndex >= 0 && selectedMaskIndex < store.getCurrentMasks().size()
                && selectedMaskIndex < canvas.getMasks().size()) {
            MaskAnnotation mask = store.getCurrentMasks().get(selectedMaskIndex);
            mask.label = label;
            Polygon polygon = canvas.getMasks().get(selectedMaskIndex);
            Text text = canvas.getMaskTexts().get(selectedMaskIndex);
            AnnotationGeometry.styleMaskPolygon(polygon, label);
            text.setText(labelDisplay.apply(label));
            text.setFill(ImageCanvas.getLabelColor(label));
            canvas.updateMaskTextPosition(selectedMaskIndex);
            canvas.selectMask(selectedMaskIndex);
            onAnnotationsChanged.run();
            return true;
        }

        if (selectedIndex < 0 || selectedIndex >= store.getCurrent().size()
                || selectedIndex >= canvas.getRects().size()) {
            return false;
        }

        Annotation ann = store.getCurrent().get(selectedIndex);
        ann.label = label;

        Color color = ImageCanvas.getLabelColor(label);
        Rectangle rect = canvas.getRects().get(selectedIndex);
        Text text = canvas.getTexts().get(selectedIndex);
        rect.setStroke(color);
        text.setText(labelDisplay.apply(label));
        text.setFill(color);
        canvas.updateTextPosition(selectedIndex);
        canvas.selectBox(selectedIndex);
        onAnnotationsChanged.run();
        return true;
    }

    private void handlePressed(MouseEvent e) {
        canvas.requestFocus();
        if (canvas.getImageView().getImage() == null) {
            return;
        }

        if (e.isSecondaryButtonDown()) {
            deleteAnnotationAt(e.getX(), e.getY());
            return;
        }

        if (selectedMode.get() == AnnotationMode.MASK) {
            int maskIndex = canvas.findTopmostMask(e.getX(), e.getY());
            if (maskIndex >= 0) {
                selectedMaskIndex = maskIndex;
                selectedIndex = -1;
                canvas.selectMask(maskIndex);
                return;
            }
            if (!AnnotationGeometry.isInsideImage(canvas, e.getX(), e.getY())) {
                resetSelection();
                return;
            }
            beginMask(e);
            return;
        }

        int boxIndex = canvas.findTopmostBox(e.getX(), e.getY());
        if (boxIndex >= 0) {
            beginEdit(boxIndex, e);
            return;
        }

        if (!AnnotationGeometry.isInsideImage(canvas, e.getX(), e.getY())) {
            resetSelection();
            return;
        }

        beginDraw(e);
    }

    private void handleDragged(MouseEvent e) {
        if (dragMode == DragMode.DRAW) {
            updateDrawingRect(e);
        } else if (dragMode == DragMode.MASK) {
            updateMask(e);
        } else if (isEditing()) {
            updateEditedRect(e);
        }
    }

    private void handleReleased(MouseEvent e) {
        if (dragMode == DragMode.DRAW) {
            finishDrawingRect();
        } else if (dragMode == DragMode.MASK) {
            finishMask();
        } else if (isEditing()) {
            finishEditingRect();
        }
        dragMode = DragMode.NONE;
    }

    private void handleMoved(MouseEvent e) {
        if (canvas.getImageView().getImage() == null || isEditing() || dragMode == DragMode.DRAW) {
            canvas.setCursor(Cursor.DEFAULT);
            return;
        }

        if (selectedMode.get() == AnnotationMode.MASK) {
            canvas.setCursor(AnnotationGeometry.isInsideImage(canvas, e.getX(), e.getY()) ? Cursor.CROSSHAIR : Cursor.DEFAULT);
            return;
        }

        int boxIndex = canvas.findTopmostBox(e.getX(), e.getY());
        if (boxIndex < 0) {
            canvas.setCursor(AnnotationGeometry.isInsideImage(canvas, e.getX(), e.getY()) ? Cursor.CROSSHAIR : Cursor.DEFAULT);
            return;
        }

        DragMode hoverMode = resolveDragMode(canvas.getRects().get(boxIndex), e.getX(), e.getY());
        canvas.setCursor(cursorForMode(hoverMode));
    }

    private void deleteAnnotationAt(double x, double y) {
        int maskIdx = canvas.findTopmostMask(x, y);
        if (maskIdx >= 0) {
            canvas.removeMask(maskIdx);
            store.removeMask(maskIdx);
            if (selectedMaskIndex == maskIdx) {
                resetSelection();
            } else if (selectedMaskIndex > maskIdx) {
                selectedMaskIndex--;
            }
            onAnnotationsChanged.run();
            return;
        }

        int idx = canvas.findTopmostBox(x, y);
        if (idx < 0) return;

        canvas.removeBox(idx);
        store.removeAnnotation(idx);
        if (selectedIndex == idx) {
            resetSelection();
        } else if (selectedIndex > idx) {
            selectedIndex--;
        }
        onAnnotationsChanged.run();
    }

    private void beginDraw(MouseEvent e) {
        resetSelection();
        dragMode = DragMode.DRAW;
        startX = AnnotationGeometry.clampX(canvas, e.getX());
        startY = AnnotationGeometry.clampY(canvas, e.getY());
        currentRect = new Rectangle(startX, startY, 0, 0);
        currentRect.setStroke(Color.web("#2563eb"));
        currentRect.setFill(Color.color(0.15, 0.39, 0.92, 0.12));
        currentRect.setStrokeWidth(2);
        canvas.getChildren().add(currentRect);
    }

    private void beginMask(MouseEvent e) {
        resetSelection();
        dragMode = DragMode.MASK;
        currentMaskPoints.clear();
        currentMask = new Polygon();
        AnnotationGeometry.styleMaskPolygon(currentMask, selectedLabel.get());
        currentMask.setMouseTransparent(true);
        canvas.getChildren().add(currentMask);
        addMaskPoint(e);
    }

    private void updateMask(MouseEvent e) {
        addMaskPoint(e);
    }

    private void addMaskPoint(MouseEvent e) {
        double x = AnnotationGeometry.clampX(canvas, e.getX());
        double y = AnnotationGeometry.clampY(canvas, e.getY());
        if (currentMaskPoints.size() >= 2) {
            double previousX = currentMaskPoints.get(currentMaskPoints.size() - 2);
            double previousY = currentMaskPoints.get(currentMaskPoints.size() - 1);
            if (Math.hypot(x - previousX, y - previousY) < MIN_MASK_POINT_DISTANCE) {
                return;
            }
        }
        currentMaskPoints.add(x);
        currentMaskPoints.add(y);
        currentMask.getPoints().setAll(currentMaskPoints);
    }

    private void finishMask() {
        if (currentMask == null) return;
        if (currentMaskPoints.size() < MIN_MASK_POINTS * 2) {
            canvas.getChildren().remove(currentMask);
            currentMask = null;
            currentMaskPoints.clear();
            return;
        }

        File imageFile = currentImageFile.get();
        if (imageFile == null) {
            canvas.getChildren().remove(currentMask);
            currentMask = null;
            currentMaskPoints.clear();
            return;
        }

        LabelClass label = selectedLabel.get();
        MaskAnnotation mask = AnnotationGeometry.maskFromCanvasPoints(imageFile, label, currentMaskPoints, canvas);
        store.addMask(mask);

        currentMask.setMouseTransparent(false);
        Text text = new Text(labelDisplay.apply(label));
        text.setFill(ImageCanvas.getLabelColor(label));
        canvas.getChildren().remove(currentMask);
        canvas.addMask(currentMask, text);
        selectedMaskIndex = canvas.getMasks().size() - 1;
        selectedIndex = -1;
        canvas.updateMaskTextPosition(selectedMaskIndex);
        canvas.selectMask(selectedMaskIndex);
        currentMask = null;
        currentMaskPoints.clear();
        onAnnotationsChanged.run();
    }

    private void updateDrawingRect(MouseEvent e) {
        if (currentRect == null) return;
        double endX = AnnotationGeometry.clampX(canvas, e.getX());
        double endY = AnnotationGeometry.clampY(canvas, e.getY());
        currentRect.setX(Math.min(startX, endX));
        currentRect.setY(Math.min(startY, endY));
        currentRect.setWidth(Math.abs(endX - startX));
        currentRect.setHeight(Math.abs(endY - startY));
    }

    private void finishDrawingRect() {
        if (currentRect == null) return;
        if (currentRect.getWidth() < MIN_BOX_SIZE || currentRect.getHeight() < MIN_BOX_SIZE) {
            canvas.getChildren().remove(currentRect);
            currentRect = null;
            return;
        }

        File imageFile = currentImageFile.get();
        if (imageFile == null) {
            canvas.getChildren().remove(currentRect);
            currentRect = null;
            return;
        }

        LabelClass label = selectedLabel.get();
        Annotation ann = AnnotationGeometry.annotationFromRectangle(imageFile, label, currentRect, canvas);
        store.addAnnotation(ann);

        Color color = ImageCanvas.getLabelColor(label);
        currentRect.setStroke(color);
        currentRect.setFill(Color.TRANSPARENT);
        Text text = new Text(labelDisplay.apply(label));
        text.setFill(color);
        canvas.addBox(currentRect, text);
        selectedIndex = canvas.getRects().size() - 1;
        canvas.updateTextPosition(selectedIndex);
        canvas.selectBox(selectedIndex);
        currentRect = null;
        onAnnotationsChanged.run();
    }

    private void beginEdit(int boxIndex, MouseEvent e) {
        selectedIndex = boxIndex;
        selectedMaskIndex = -1;
        canvas.selectBox(selectedIndex);
        Rectangle rect = canvas.getRects().get(selectedIndex);
        dragMode = resolveDragMode(rect, e.getX(), e.getY());
        dragStartX = e.getX();
        dragStartY = e.getY();
        startRectX = rect.getX();
        startRectY = rect.getY();
        startRectW = rect.getWidth();
        startRectH = rect.getHeight();
    }

    private void updateEditedRect(MouseEvent e) {
        if (selectedIndex < 0 || selectedIndex >= canvas.getRects().size()) return;
        Rectangle rect = canvas.getRects().get(selectedIndex);
        Bounds bounds = AnnotationGeometry.imageBounds(canvas);
        if (bounds == null) return;

        double mouseX = AnnotationGeometry.clampX(canvas, e.getX());
        double mouseY = AnnotationGeometry.clampY(canvas, e.getY());
        double left = startRectX;
        double top = startRectY;
        double right = startRectX + startRectW;
        double bottom = startRectY + startRectH;

        if (dragMode == DragMode.MOVE) {
            double dx = e.getX() - dragStartX;
            double dy = e.getY() - dragStartY;
            left = clamp(startRectX + dx, bounds.getMinX(), bounds.getMaxX() - startRectW);
            top = clamp(startRectY + dy, bounds.getMinY(), bounds.getMaxY() - startRectH);
            right = left + startRectW;
            bottom = top + startRectH;
        } else {
            if (dragMode == DragMode.RESIZE_W || dragMode == DragMode.RESIZE_NW || dragMode == DragMode.RESIZE_SW) {
                left = clamp(mouseX, bounds.getMinX(), right - MIN_BOX_SIZE);
            }
            if (dragMode == DragMode.RESIZE_E || dragMode == DragMode.RESIZE_NE || dragMode == DragMode.RESIZE_SE) {
                right = clamp(mouseX, left + MIN_BOX_SIZE, bounds.getMaxX());
            }
            if (dragMode == DragMode.RESIZE_N || dragMode == DragMode.RESIZE_NW || dragMode == DragMode.RESIZE_NE) {
                top = clamp(mouseY, bounds.getMinY(), bottom - MIN_BOX_SIZE);
            }
            if (dragMode == DragMode.RESIZE_S || dragMode == DragMode.RESIZE_SW || dragMode == DragMode.RESIZE_SE) {
                bottom = clamp(mouseY, top + MIN_BOX_SIZE, bounds.getMaxY());
            }
        }

        rect.setX(left);
        rect.setY(top);
        rect.setWidth(right - left);
        rect.setHeight(bottom - top);
        canvas.updateTextPosition(selectedIndex);
        AnnotationGeometry.updateAnnotationFromRectangle(store.getCurrent().get(selectedIndex), rect, canvas);
    }

    private void finishEditingRect() {
        if (selectedIndex >= 0 && selectedIndex < canvas.getRects().size()) {
            AnnotationGeometry.updateAnnotationFromRectangle(
                    store.getCurrent().get(selectedIndex),
                    canvas.getRects().get(selectedIndex),
                    canvas
            );
            onAnnotationsChanged.run();
        }
    }

    private DragMode resolveDragMode(Rectangle rect, double x, double y) {
        boolean nearLeft = Math.abs(x - rect.getX()) <= RESIZE_HANDLE_SIZE;
        boolean nearRight = Math.abs(x - (rect.getX() + rect.getWidth())) <= RESIZE_HANDLE_SIZE;
        boolean nearTop = Math.abs(y - rect.getY()) <= RESIZE_HANDLE_SIZE;
        boolean nearBottom = Math.abs(y - (rect.getY() + rect.getHeight())) <= RESIZE_HANDLE_SIZE;

        if (nearLeft && nearTop) return DragMode.RESIZE_NW;
        if (nearRight && nearTop) return DragMode.RESIZE_NE;
        if (nearLeft && nearBottom) return DragMode.RESIZE_SW;
        if (nearRight && nearBottom) return DragMode.RESIZE_SE;
        if (nearLeft) return DragMode.RESIZE_W;
        if (nearRight) return DragMode.RESIZE_E;
        if (nearTop) return DragMode.RESIZE_N;
        if (nearBottom) return DragMode.RESIZE_S;
        return DragMode.MOVE;
    }

    private boolean isEditing() {
        return dragMode != DragMode.NONE && dragMode != DragMode.DRAW && dragMode != DragMode.MASK;
    }

    private Cursor cursorForMode(DragMode mode) {
        return switch (mode) {
            case MOVE -> Cursor.MOVE;
            case RESIZE_N -> Cursor.N_RESIZE;
            case RESIZE_E -> Cursor.E_RESIZE;
            case RESIZE_S -> Cursor.S_RESIZE;
            case RESIZE_W -> Cursor.W_RESIZE;
            case RESIZE_NE -> Cursor.NE_RESIZE;
            case RESIZE_NW -> Cursor.NW_RESIZE;
            case RESIZE_SE -> Cursor.SE_RESIZE;
            case RESIZE_SW -> Cursor.SW_RESIZE;
            default -> Cursor.DEFAULT;
        };
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
