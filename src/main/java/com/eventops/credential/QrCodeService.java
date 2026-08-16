package com.eventops.credential;

import com.eventops.shared.BusinessRuleException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.springframework.stereotype.Service;

@Service
public class QrCodeService {

    public byte[] gerar(String conteudo) {
        try {
            var matriz = new QRCodeWriter().encode(conteudo, BarcodeFormat.QR_CODE, 360, 360);
            var saida = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matriz, "PNG", saida);
            return saida.toByteArray();
        } catch (WriterException | IOException excecao) {
            throw new BusinessRuleException("QRCODE_NAO_GERADO", "Nao foi possivel gerar o QR Code.");
        }
    }
}
