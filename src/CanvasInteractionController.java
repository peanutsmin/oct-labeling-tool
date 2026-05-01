import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.io.File;
import java.util.function.Function;
import java.util.function.Supplier;

public class CanvasInteractionController {
    private final ImageCanvas canvas;
    private final AnnotationStore store;
    private final Supplier<File> currentImageFile;
    private final Supplier<LabelClass> selectedLabel;
    private final Function<LabelClass, String> labelDisplay;
    private final Runnable onAnnotationsChanged;

    private Rectangle currentRect;
    private DragMode dragMode = DragMode.NONE;
    private int selectedIndex = -1;
    private double startX, startY;
    private double dragStartX, dragStartY;
    private double startRectX, startRectY, startRectW, startRectH;

    private static final double MIN_BOX_SIZE = 5.0;
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
        RESIZE_SW
    }

    public CanvasInteractionController(
            ImageCanvas canvas,
            AnnotationStore store,
            Supplier<File> currentImageFile,
            Supplier<LabelClass> selectedLabel,
            Function<LabelClass, String> labelDisplay,
            Runnable onAnnotationsChanged
    ) {
        this.canvas = canvas;
        this.store = store;
        this.currentImageFile = currentImageFile;
        this.selectedLabel = selectedLabel;
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
        dragMode = DragMode.NONE;
        currentRect = null;
        canvas.clearSelection();
    }

    public boolean applySelectedLabel(LabelClass label) {
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
            deleteBoxAt(e.getX(), e.getY());
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
        } else if (isEditing()) {
            updateEditedRect(e);
        }
    }

    private void handleReleased(MouseEvent e) {
        if (dragMode == DragMode.DRAW) {
            finishDrawingRect();
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

        int boxIndex = canvas.findTopmostBox(e.getX(), e.getY());
        if (boxIndex < 0) {
            canvas.setCursor(AnnotationGeometry.isInsideImage(canvas, e.getX(), e.getY()) ? Cursor.CROSSHAIR : Cursor.DEFAULT);
            return;
        }

        DragMode hoverMode = resolveDragMode(canvas.getRects().get(boxIndex), e.getX(), e.getY());
        canvas.setCursor(cursorForMode(hoverMode));
    }

    private void deleteBoxAt(double x, double y) {
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
        return dragMode != DragMode.NONE && dragMode != DragMode.DRAW;
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
