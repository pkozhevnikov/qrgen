package home.training.qrgen;

import org.apache.commons.io.IOUtils;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.InputStreamCallback;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by techno on 10/22/15.
 */
public class QRGenerator extends AbstractProcessor {

    @Override
    public void onTrigger(ProcessContext processContext, ProcessSession processSession) throws ProcessException {
        FlowFile file = processSession.get();
        SimpleInCallback callback = new SimpleInCallback();
        processSession.read(file, callback);

    }

    private BufferedImage generate(String content) {
        return null;
    }

    private class SimpleInCallback implements InputStreamCallback {
        private String content;
        @Override
        public void process(InputStream in) throws IOException {
            content = IOUtils.toString(in);
        }
        public String getContent() {
            return content;
        }
    }
}

