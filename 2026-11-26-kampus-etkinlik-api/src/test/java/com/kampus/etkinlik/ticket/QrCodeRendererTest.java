package com.kampus.etkinlik.ticket;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class QrCodeRendererTest {

  @Test
  void uretilen_png_geri_okunabilir_ve_kodu_tasir() throws Exception {
    String code = new TicketCodeGenerator().next();

    byte[] png = new QrCodeRenderer().png(code);

    var image = ImageIO.read(new ByteArrayInputStream(png));
    var bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
    assertThat(new QRCodeReader().decode(bitmap).getText()).isEqualTo(code);
  }

  @Test
  void bilet_kodlari_benzersiz_ve_url_guvenli() {
    var gen = new TicketCodeGenerator();
    String a = gen.next();
    String b = gen.next();

    assertThat(a).isNotEqualTo(b).hasSize(43).matches("[A-Za-z0-9_-]+");
  }
}
