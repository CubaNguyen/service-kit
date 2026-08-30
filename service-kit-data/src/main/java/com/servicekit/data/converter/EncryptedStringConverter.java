package com.servicekit.data.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * JPA AttributeConverter mã hóa/giải mã dữ liệu nhạy cảm (PII) tự động khi đọc/ghi DB.
 *
 * Thuật toán: AES-256-GCM (Authenticated Encryption)
 * - AES-256: Mã hóa mạnh 256-bit
 * - GCM mode: Chống tampering (xác thực tính toàn vẹn của dữ liệu)
 * - IV (Initialization Vector) 12 byte sinh ngẫu nhiên mỗi lần mã hóa → mỗi lần ghi ra DB
 *   giá trị ciphertext sẽ khác nhau dù plaintext giống nhau (Semantic Security)
 *
 * Cách dùng:
 * <pre>
 *   {@literal @}Convert(converter = EncryptedStringConverter.class)
 *   {@literal @}Column(name = "national_id")
 *   private String nationalId; // Lưu DB dạng mã hóa, đọc ra là plaintext
 * </pre>
 *
 * Cấu hình khóa bí mật:
 * Đặt biến môi trường hoặc application.yml:
 * <pre>
 *   service-kit:
 *     data:
 *       encryption:
 *         secret-key: "base64-encoded-32-bytes-key"  # 32 bytes = 256 bits
 * </pre>
 *
 * Tạo khóa mẫu:
 * <pre>
 *   openssl rand -base64 32
 * </pre>
 *
 * LƯU Ý:
 * - Không bao giờ hardcode key trong source code hoặc commit lên Git
 * - Quản lý key qua AWS KMS / HashiCorp Vault / Kubernetes Secret
 * - Nếu mất key → mất toàn bộ dữ liệu đã mã hóa (không thể khôi phục)
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;       // 96 bits IV (recommended for GCM)
    private static final int GCM_TAG_LENGTH = 128;     // 128-bit authentication tag

    /**
     * Đọc secret key từ biến môi trường.
     * Key phải là chuỗi Base64 của 32 bytes (256 bits).
     *
     * Thứ tự ưu tiên:
     * 1. Environment variable: SERVICE_KIT_ENCRYPTION_KEY
     * 2. System property: service-kit.data.encryption.secret-key
     */
    private SecretKey getSecretKey() {
        String base64Key = System.getenv("SERVICE_KIT_ENCRYPTION_KEY");
        if (base64Key == null) {
            base64Key = System.getProperty("service-kit.data.encryption.secret-key");
        }
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException(
                "[service-kit-data] Encryption key not configured. " +
                "Set environment variable SERVICE_KIT_ENCRYPTION_KEY or " +
                "system property service-kit.data.encryption.secret-key"
            );
        }
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Mã hóa plaintext → Base64(IV + CipherText + GCM_TAG)
     * Format lưu DB: base64( [12 bytes IV] + [ciphertext + 16 bytes GCM tag] )
     */
    @Override
    public String convertToDatabaseColumn(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] cipherText = cipher.doFinal(plainText.getBytes());

            // Ghép IV + CipherText vào 1 mảng để lưu
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());

        } catch (Exception e) {
            throw new IllegalStateException("[service-kit-data] Failed to encrypt field value", e);
        }
    }

    /**
     * Giải mã Base64(IV + CipherText + GCM_TAG) → plaintext
     */
    @Override
    public String convertToEntityAttribute(String encryptedBase64) {
        if (encryptedBase64 == null) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedBase64);

            // Tách IV ra khỏi ciphertext
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            return new String(cipher.doFinal(cipherText));

        } catch (Exception e) {
            throw new IllegalStateException("[service-kit-data] Failed to decrypt field value", e);
        }
    }
}
