package hr.pickBox.servis;

import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import hr.antikvarijat.model.ProdajaStavka;
import hr.antikvarijat.model.ProdajaZaglavlje;
import hr.antikvarijat.service.ProdajaStavkaService;
import hr.antikvarijat.service.ProdajaZaglavljeService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static hr.antikvarijat.servis.QRCodeGenerator.createPDf417;
import static hr.antikvarijat.servis.QRCodeGenerator.createQR;

public class PdfGenerator {
    private final ProdajaZaglavljeService prodajaZaglavljeService;

    private final ProdajaStavkaService prodajaStavkaService;

    public PdfGenerator(ProdajaZaglavljeService prodajaZaglavljeService, ProdajaStavkaService prodajaStavkaService) {
        this.prodajaZaglavljeService = prodajaZaglavljeService;
        this.prodajaStavkaService = prodajaStavkaService;
    }

    public void generateInvoice(int idProdajaZaglavlje) {


        // Dohvatite podatke o prodaji na temelju idProdajaZaglavlje
        ProdajaZaglavlje prodajaZaglavlje = prodajaZaglavljeService.getProdajaZaglavljeById(idProdajaZaglavlje);

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            PDType0Font font = PDType0Font.load(document, new File("font/Roboto-Medium.ttf"));

            PDPageContentStream contentStream = new PDPageContentStream(document, page);


            contentStream.setFont(font, 12);
//            contentStream.setLeading(14.5f);

            // Postavite položaj za ispis teksta
            float y = page.getMediaBox().getHeight() - 50;

            // Ispišite naslov računa
            contentStream.beginText();
            contentStream.newLineAtOffset(50, y);
            contentStream.showText("Web antikvarijat");
            contentStream.endText();
            y -= 20; // manji razmak do linije

            contentStream.setFont(font, 10);
            contentStream.beginText();
            contentStream.newLineAtOffset(50, y);
            contentStream.showText("Ulica jablanova bb, Osijek, OIB: 53698987365");
            contentStream.endText();
            y -= 10; // manji razmak do linije

            //  crta liniju
            contentStream.setStrokingColor(Color.GRAY);
            contentStream.setLineWidth(0.5f);
            contentStream.moveTo(50, y);
            contentStream.lineTo(562, y);
            contentStream.stroke();
            y -= 30;

            // Ispišite ostale podatke o prodaji

            contentStream.setFont(font, 10);
            if (prodajaZaglavlje.getPartner() != null) {
                contentStream.beginText();
                contentStream.newLineAtOffset(50, y);
                contentStream.showText("Kupac:");
                contentStream.endText();
                y -= 20;
                contentStream.beginText();
                contentStream.newLineAtOffset(50, y);
                contentStream.showText(prodajaZaglavlje.getPartner().getNazivPartnera());
                contentStream.endText();
                y -= 20;
                contentStream.beginText();
                contentStream.newLineAtOffset(50, y);
                contentStream.showText(prodajaZaglavlje.getPartner().getUlicaBroj());
                contentStream.endText();
                y -= 20;
                contentStream.beginText();
                contentStream.newLineAtOffset(50, y);
                contentStream.showText(prodajaZaglavlje.getPartner().getGrad().getPostanskiBroj() + " " + prodajaZaglavlje.getPartner().getGrad().getNazivGrada());
                contentStream.endText();
                y -= 20;
                contentStream.beginText();
                contentStream.newLineAtOffset(50, y);
                contentStream.showText("OIB: " + prodajaZaglavlje.getPartner().getOib());
                contentStream.endText();
                y -= 20;
            }


            String racunBroj = prodajaZaglavlje.getIdProdajaZaglavlje() + "-1-1";
            y -= 20;
            contentStream.setFont(font, 12);
            contentStream.beginText();
            contentStream.newLineAtOffset(50, y);
            contentStream.showText("Račun broj: " + racunBroj);
            contentStream.endText();
            // y -= 20;
            contentStream.setFont(font, 10);
            contentStream.beginText();
            contentStream.newLineAtOffset(350, y);

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
            String croDatum = dateFormat.format(prodajaZaglavlje.getDatumProdaje());
            contentStream.showText("Datum izdavanja računa: " + croDatum);
            contentStream.endText();
            y -= 20;

            List<ProdajaStavka> listaProdajaStavki = prodajaStavkaService.getProdajaStavkeZaRacun(idProdajaZaglavlje);

            contentStream.setFont(font, 8);
            contentStream.setStrokingColor(Color.GRAY);
            contentStream.setLineWidth(0.5f);
            int pageWidth = (int) page.getTrimBox().getWidth(); //get width of the page
            int pageHeight = (int) page.getTrimBox().getHeight(); //get height of the page

            int initX = 50;
//            int initY = pageHeight - 50;
            float initY = y;
            int cellHeight = 20;
            int cellWidth = 70;

            int colCount = 5;
            int rowCount = listaProdajaStavki.size() + 1;

            for (int i = 1; i <= rowCount; i++) {
                for (int j = 1; j <= colCount; j++) {

                    if (i == 1) {

                        if (j == 1) {
                            int sirina = 30;
                            contentStream.addRect(initX, initY, sirina, -cellHeight);
                            contentStream.beginText();
                            contentStream.newLineAtOffset(initX + 10, initY - cellHeight + 7);
                            contentStream.showText("RB");
                            contentStream.endText();
                            initX += sirina;

                        } else if (j == 2) {
                            int sirina = 200;
                            contentStream.addRect(initX, initY, cellWidth + sirina, -cellHeight);
                            contentStream.beginText();
                            contentStream.newLineAtOffset(initX + 10, initY - cellHeight + 7);
                            contentStream.showText("Autor i naslov knjige");
                            contentStream.endText();
                            initX += cellWidth + sirina;

                        } else if (j == 3) {
                            contentStream.addRect(initX, initY, cellWidth, -cellHeight);
                            contentStream.beginText();
                            contentStream.newLineAtOffset(initX + 15, initY - cellHeight + 7);
                            contentStream.showText("Količina");
                            contentStream.endText();
                            initX += cellWidth;

                        } else if (j == 4) {
                            contentStream.addRect(initX, initY, cellWidth, -cellHeight);
                            contentStream.beginText();
                            contentStream.newLineAtOffset(initX + 15, initY - cellHeight + 7);
                            contentStream.showText("Cijena");
                            contentStream.endText();
                            initX += cellWidth;

                        } else if (j == 5) {
                            contentStream.addRect(initX, initY, cellWidth, -cellHeight);
                            contentStream.beginText();
                            contentStream.newLineAtOffset(initX + 15, initY - cellHeight + 7);
//                            contentStream.setFont(font, 10);
                            contentStream.showText("Iznos");
                            contentStream.endText();
                            initX += cellWidth;

                        }


                    } else {

                        ProdajaStavka prodajaStavka = listaProdajaStavki.get(i - 2);

                        if (j == 1) {
                            int sirina = 30;
                            contentStream.addRect(initX, initY, sirina, -cellHeight);
                            contentStream.beginText();
                            contentStream.newLineAtOffset(initX + 11, initY - cellHeight + 7);
                            contentStream.showText(String.valueOf(i - 1) + ".");
                            contentStream.endText();
                            initX += sirina;

                        } else if (j == 2) {
                            int sirina = 200;
                            contentStream.addRect(initX, initY, cellWidth + sirina, -cellHeight);
                            contentStream.beginText();
                            contentStream.newLineAtOffset(initX + 10, initY - cellHeight + 7);
                            contentStream.showText(prodajaStavka.getKnjiga().getAutor().getNazivAutora() + ": " + prodajaStavka.getKnjiga().getNazivKnjige());
                            contentStream.endText();
                            initX += cellWidth + sirina;

                        } else if (j == 3) {
                            contentStream.addRect(initX, initY, cellWidth, -cellHeight);
                            contentStream.beginText();
                            contentStream.newLineAtOffset(initX + 30, initY - cellHeight + 7);
                            contentStream.showText(String.valueOf(prodajaStavka.getKolicina()));
                            contentStream.endText();
                            initX += cellWidth;

                        } else if (j == 4) {
                            contentStream.addRect(initX, initY, cellWidth, -cellHeight);
                            contentStream.beginText();
                            contentStream.newLineAtOffset(initX + 20, initY - cellHeight + 7);
                            contentStream.showText(String.valueOf(prodajaStavka.getCijenaProdaje()));
                            contentStream.endText();
                            initX += cellWidth;

                        } else if (j == 5) {
                            contentStream.addRect(initX, initY, cellWidth, -cellHeight);
                            contentStream.beginText();
                            contentStream.newLineAtOffset(initX + 15, initY - cellHeight + 7);
                            BigDecimal cijenaProdaje = prodajaStavka.getCijenaProdaje();
                            int kolicina = prodajaStavka.getKolicina();
                            BigDecimal kolicinaBigDecimal = BigDecimal.valueOf(kolicina);
                            BigDecimal iznosStavke = cijenaProdaje.multiply(kolicinaBigDecimal);
                            contentStream.showText(String.valueOf(iznosStavke));
                            contentStream.endText();
                            initX += cellWidth;

                        }
                    }
                }
                initX = 50;
                initY -= cellHeight;
            }

            contentStream.stroke();
            y = initY - 20;

            BigDecimal zbrojIznosa = BigDecimal.valueOf(0);
            for (ProdajaStavka stavka : listaProdajaStavki) {
                BigDecimal cijenaProdaje = stavka.getCijenaProdaje();
                int kolicina = stavka.getKolicina();

                BigDecimal kolicinaBigDecimal = BigDecimal.valueOf(kolicina);
                BigDecimal iznosStavke = cijenaProdaje.multiply(kolicinaBigDecimal);

                zbrojIznosa = zbrojIznosa.add(iznosStavke);
            }

            BigDecimal zbrojHrk = zbrojIznosa.multiply(new BigDecimal("7.53450")).setScale(2, BigDecimal.ROUND_HALF_UP);

            contentStream.setFont(font, 10);

            contentStream.beginText();
            contentStream.newLineAtOffset(400, y);
            contentStream.showText("Ukupan iznos računa:  " + zbrojIznosa + " EUR");
            contentStream.endText();
            y -= 20;
            contentStream.beginText();
            contentStream.newLineAtOffset(362, y);
            contentStream.showText("(Tečaj 1 EUR = 7,53450 HRK) " + zbrojHrk + " HRK");
            contentStream.endText();
            y -= 20;


            contentStream.setFont(font, 10);
            contentStream.beginText();
            contentStream.newLineAtOffset(50, y);
            contentStream.showText("Oznaka operatera: " + prodajaZaglavlje.getUser().getName());
            contentStream.endText();
            y -= 20;

            contentStream.beginText();
            contentStream.newLineAtOffset(50, y);
            contentStream.showText("Način plaćanja: " + prodajaZaglavlje.getNacinPlacanja().getNazivNacinaPlacanja());
            contentStream.endText();
            y -= 20;

            // QR Code

            QRCodeGenerator qrCodeGenerator = new QRCodeGenerator();
            String data = "www.betastudio.hr";

            // The path where the image will get saved
            String path = "dat/beta_studio.png";

            // Encoding charset
            String charset = "utf-8";

            Map<EncodeHintType, ErrorCorrectionLevel> hashMap
                    = new HashMap<EncodeHintType,
                    ErrorCorrectionLevel>();

            hashMap.put(EncodeHintType.ERROR_CORRECTION,
                    ErrorCorrectionLevel.L);

            // Create the QR code and save
            // in the specified folder
            // as a jpg file
            createQR(data, path, charset, hashMap, 200, 200);


            PDImageXObject qrCodeImage = LosslessFactory.createFromImage(document, loadImage(path));

            // Promjena veličine slike QR koda ako je potrebno
            float qrCodeImageWidth = qrCodeImage.getWidth();
            float qrCodeImageHeight = qrCodeImage.getHeight();
//            float desiredWidth = 200;  // željena širina slike u PDF-u
//            float desiredHeight = (desiredWidth / qrCodeImageWidth) * qrCodeImageHeight;

            float desiredWidth = 100;
            float desiredHeight = 100;

            // Postavljanje pozicije slike QR koda u PDF-u
            float x = 50;  // X koordinata slike u PDF-u
            y -= 100;
            contentStream.drawImage(qrCodeImage, x, y, desiredWidth, desiredHeight);

            // 2d barCode

            if (prodajaZaglavlje.getPartner() != null && Objects.equals(prodajaZaglavlje.getNacinPlacanja().getNazivNacinaPlacanja().toLowerCase(), "virman")) {


                BigInteger cijeliDio = zbrojIznosa.toBigInteger();
                BigDecimal decimalniDio = zbrojIznosa.remainder(BigDecimal.ONE).movePointRight(zbrojIznosa.scale());

                DecimalFormat decimalFormat = new DecimalFormat("00");
                String formatiraniDecimalniDio = decimalFormat.format(decimalniDio);
                String decimalniDioString = decimalFormat.format(decimalniDio);

                String cijeliDioString = cijeliDio.toString();

                String rezultat = cijeliDioString + decimalniDioString;

                String formatiraniRezultat = String.format("%014d", Integer.parseInt(rezultat));

                QRCodeGenerator pdf417CodeGenerator = new QRCodeGenerator();
                String tekst = "HRVHUB30\n";
                tekst += "EUR\n";
                tekst += formatiraniRezultat + "\n";
                tekst += prodajaZaglavlje.getPartner().getNazivPartnera() + "\n";
                tekst += prodajaZaglavlje.getPartner().getUlicaBroj() + "\n";
                tekst += prodajaZaglavlje.getPartner().getGrad().getNazivGrada() + "\n";
                tekst += "Web antikvarijat\n";
                tekst += "Ulica jablanova bb\n";
                tekst += "Osijek\n";
                tekst += "HR2023600001101441703\n";
                tekst += "HR00\n";
                tekst += racunBroj + "\n";
                tekst += "GDSV\n";
                tekst += "Plaćanje računa broj: " + racunBroj ;


                String file_path = "dat/pdf417.png";
                createPDf417(tekst, file_path, charset, hashMap, 100, 220);
                PDImageXObject pdf417CodeImage = LosslessFactory.createFromImage(document, loadImage(file_path));
                contentStream.drawImage(pdf417CodeImage, 350, y, 220, 100);

            }


            // Zatvorite PDPageContentStream
            contentStream.close();

            // Spremite PDF dokument na odredište
            document.save("pdf/prodaja.pdf");


        } catch (IOException e) {
            e.printStackTrace();
        } catch (WriterException e) {
            throw new RuntimeException(e);
        }
    }

    public static void creteTablePdf() throws IOException {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        document.addPage(page);

        PDType0Font font = PDType0Font.load(document, new File("font/Roboto-Medium.ttf"));

        int pageWidth = (int) page.getTrimBox().getWidth(); //get width of the page
        int pageHeight = (int) page.getTrimBox().getHeight(); //get height of the page

        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        contentStream.setStrokingColor(Color.DARK_GRAY);
        contentStream.setLineWidth(1);

        int initX = 50;
        int initY = pageHeight - 50;
        int cellHeight = 20;
        int cellWidth = 100;

        int colCount = 5;
        int rowCount = 3;

        for (int i = 1; i <= rowCount; i++) {
            for (int j = 1; j <= colCount; j++) {
                if (j == 2) {
                    int sirina2 = 50;
                    contentStream.addRect(initX, initY, cellWidth + sirina2, -cellHeight);

                    contentStream.beginText();
                    contentStream.newLineAtOffset(initX + 30, initY - cellHeight + 7);
                    contentStream.setFont(font, 10);
                    contentStream.showText("tablica [" + i + "," + j + "]");
                    contentStream.endText();

                    initX += cellWidth + sirina2;
                } else {
                    contentStream.addRect(initX, initY, cellWidth, -cellHeight);

                    contentStream.beginText();
                    contentStream.newLineAtOffset(initX + 10, initY - cellHeight + 7);
                    contentStream.setFont(font, 10);
                    contentStream.showText("tablica [" + i + "," + j + "]");
                    contentStream.endText();

                    initX += cellWidth;
                }

            }
            initX = 50;
            initY -= cellHeight;
        }

        contentStream.stroke();
        contentStream.close();


        document.save("pdf/table.pdf");
        document.close();
        System.out.println("table pdf created");
    }


    public void generatePDFWithQRCode(String qrCodeImagePath, String pdfPath) {
        try {
            PDDocument document = new PDDocument();
            PDPage page = new PDPage();
            document.addPage(page);

            PDImageXObject qrCodeImage = LosslessFactory.createFromImage(document, loadImage(qrCodeImagePath));
            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            // Promjena veličine slike QR koda ako je potrebno
            float qrCodeImageWidth = qrCodeImage.getWidth();
            float qrCodeImageHeight = qrCodeImage.getHeight();
            float desiredWidth = 200;  // željena širina slike u PDF-u
            float desiredHeight = (desiredWidth / qrCodeImageWidth) * qrCodeImageHeight;

            // Postavljanje pozicije slike QR koda u PDF-u
            float x = 100;  // X koordinata slike u PDF-u
            float y = 500;  // Y koordinata slike u PDF-u

            contentStream.drawImage(qrCodeImage, x, y, desiredWidth, desiredHeight);
            contentStream.close();

            document.save(pdfPath);
            document.close();
        } catch (IOException e) {
            e.printStackTrace();
            // Ovdje možete dodati svoju logiku za rukovanje greškama prilikom generiranja PDF-a
        }
    }

    private BufferedImage loadImage(String imagePath) throws IOException {
        File file = new File(imagePath);
        return ImageIO.read(file);
    }


    public static void main(String[] args) throws IOException {
        creteTablePdf();
    }
}
