package com.peanutsmin.octlabeling;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomOutputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DicomImageLoaderTest {
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void loadsUncompressedGrayscaleDicom() throws Exception {
        File dicom = new File(temp.getRoot(), "sample.dcm");
        writeDicom(dicom);

        Image image = DicomImageLoader.load(dicom);

        assertEquals(2, (int) image.getWidth());
        assertEquals(2, (int) image.getHeight());
        Color dark = image.getPixelReader().getColor(0, 0);
        Color bright = image.getPixelReader().getColor(1, 1);
        assertTrue(dark.getBrightness() < bright.getBrightness());
    }

    private void writeDicom(File file) throws Exception {
        Attributes dataset = new Attributes();
        dataset.setString(Tag.SOPClassUID, VR.UI, UID.SecondaryCaptureImageStorage);
        dataset.setString(Tag.SOPInstanceUID, VR.UI, "1.2.826.0.1.3680043.10.54321.1");
        dataset.setString(Tag.PhotometricInterpretation, VR.CS, "MONOCHROME2");
        dataset.setInt(Tag.SamplesPerPixel, VR.US, 1);
        dataset.setInt(Tag.Rows, VR.US, 2);
        dataset.setInt(Tag.Columns, VR.US, 2);
        dataset.setInt(Tag.BitsAllocated, VR.US, 8);
        dataset.setInt(Tag.BitsStored, VR.US, 8);
        dataset.setInt(Tag.HighBit, VR.US, 7);
        dataset.setInt(Tag.PixelRepresentation, VR.US, 0);
        dataset.setString(Tag.WindowCenter, VR.DS, "128");
        dataset.setString(Tag.WindowWidth, VR.DS, "256");
        dataset.setBytes(Tag.PixelData, VR.OB, new byte[]{0, 64, (byte) 128, (byte) 255});

        try (DicomOutputStream output = new DicomOutputStream(file)) {
            output.writeDataset(dataset.createFileMetaInformation(UID.ExplicitVRLittleEndian), dataset);
        }
    }
}
