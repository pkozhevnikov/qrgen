package home.training.qrgen;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ProcessorLog;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.InputStreamCallback;
import org.apache.nifi.processor.io.OutputStreamCallback;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Tags({"extract", "qr code", "text", "image"})
@CapabilityDescription("Decodes QR code")
public class QRExtractor extends AbstractProcessor {

    private static final Relationship REL_EXTRACTED = new Relationship.Builder()
            .name("extracted")
            .description("Decoded content will be sent to this destination")
            .build();


    private Set<Relationship> relationships = Collections.singleton(REL_EXTRACTED);

    @Override
    public Set<Relationship> getRelationships() {
        return relationships;
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        final ProcessorLog log = getLogger();
        final FlowFile qrFile = session.get();
        final AtomicBoolean error = new AtomicBoolean(false);
        FlowFile stringFile = session.write(session.create(), new OutputStreamCallback() {
            @Override
            public void process(final OutputStream out) throws IOException {
                try {
                    session.read(qrFile, new InputStreamCallback() {
                        @Override
                        public void process(InputStream in) throws IOException {
                            try {
                                out.write(extract(ImageIO.read(in)).getBytes());
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
        session.remove(qrFile);
        if (error.get()) {
            session.remove(stringFile);
        } else {
            session.transfer(stringFile, REL_EXTRACTED);
        }
    }

    String extract(BufferedImage image) {
        try {
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
            Map<EncodeHintType, ErrorCorrectionLevel> hintMap = new HashMap<>();
            hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
            Result result = new MultiFormatReader().decode(bitmap);
            return result.getText();
        } catch (Exception ex) {
            throw new ProcessException(ex);
        }
    }

}
