package home.training.qrgen;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.apache.commons.io.IOUtils;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ProcessorLog;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.InputStreamCallback;
import org.apache.nifi.processor.io.OutputStreamCallback;
import org.apache.nifi.processor.util.StandardValidators;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Tags({"generate", "qr code", "text", "image"})
@CapabilityDescription("Generates QR code image from string value")
public class QRGenerator extends AbstractProcessor {

    private static final Relationship REL_GENERATED = new Relationship.Builder()
            .name("generated")
            .description("Generated QR code will be sent to this destination")
            .build();

    private static final PropertyDescriptor PROP_IMAGE_SIZE = new PropertyDescriptor.Builder()
            .name("imagesize")
            .displayName("Image size")
            .description("Size of generated QR code image")
            .required(true)
            .defaultValue("200")
            .addValidator(StandardValidators.POSITIVE_INTEGER_VALIDATOR)
            .build();

    private Set<Relationship> relationships = Collections.singleton(REL_GENERATED);
    private List<PropertyDescriptor> properties = Collections.singletonList(PROP_IMAGE_SIZE);

    @Override
    public void onTrigger(final ProcessContext processContext, final ProcessSession processSession) throws ProcessException {

        final ProcessorLog log = getLogger();
        final FlowFile stringFile = processSession.get();
        final AtomicBoolean error = new AtomicBoolean(false);
        FlowFile qrFile = processSession.write(processSession.create(), new OutputStreamCallback() {
            @Override
            public void process(final OutputStream out) throws IOException {
                try {
                    processSession.read(stringFile, new InputStreamCallback() {
                        @Override
                        public void process(InputStream in) throws IOException {
                            try {
                                ImageIO.write(generate(IOUtils.toString(in),
                                        processContext.getProperty(PROP_IMAGE_SIZE).asInteger()), "PNG", out);
                            } catch (Exception ex) {
                                log.error(ex.getMessage(), ex);
                                error.set(true);
                            }
                        }
                    });
                } catch (Exception ex) {
                    log.error(ex.getMessage(), ex);
                    error.set(true);
                }
            }
        });
        processSession.remove(stringFile);
        if (error.get()) {
            processSession.remove(qrFile);
        } else {
            processSession.transfer(qrFile, REL_GENERATED);
        }
    }

    @Override
    public Set<Relationship> getRelationships() {
        return relationships;
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return properties;
    }

    BufferedImage generate(String content, int size) {
        try {
            Map<EncodeHintType, ErrorCorrectionLevel> hintMap = new HashMap<>();
            hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
            BitMatrix matrix = new MultiFormatWriter().encode(
                    content, BarcodeFormat.QR_CODE, size, size, hintMap);
            return MatrixToImageWriter.toBufferedImage(matrix);
        } catch (Exception e) {
            throw new ProcessException(e);
        }
    }

}

