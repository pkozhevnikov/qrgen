package home.training.qrgen;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class GenTest {

    private File imageFile = new File(System.getProperty("java.io.tmpdir"), "qrimagetest.png");

    @BeforeClass
    public void setup() {

    }

    @AfterClass
    public void teardown() {
        imageFile.delete();
    }

    @Test
    public void generatedOk() throws IOException {
        QRGenerator generator = new QRGenerator();
        BufferedImage image = generator.generate("hello test", 200);
        ImageIO.write(image, "PNG", imageFile);

        image = ImageIO.read(imageFile);
        QRExtractor extractor = new QRExtractor();
        Assert.assertEquals(extractor.extract(image), "hello test");
    }

}
