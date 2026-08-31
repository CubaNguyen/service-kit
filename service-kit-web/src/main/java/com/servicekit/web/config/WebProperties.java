package com.servicekit.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình tập trung cho module service-kit-web.
 *
 * <p>Khai báo trong {@code application.yml} của service sử dụng:
 * <pre>
 * service-kit:
 *   web:
 *     pagination:
 *       max-page-size: 200  # Mặc định: 100
 * </pre>
 */
@ConfigurationProperties(prefix = "service-kit.web")
public class WebProperties {

    private final Pagination pagination = new Pagination();

    public Pagination getPagination() {
        return pagination;
    }

    public static class Pagination {

        /**
         * Giới hạn số lượng phần tử tối đa trên mỗi trang.
         *
         * <p>Mọi request gửi {@code pageSize} vượt quá giá trị này sẽ bị
         * Spring Data tự động giảm xuống (clamped), không ném lỗi về client.
         * Điều này bảo vệ DB khỏi các query kiểu {@code pageSize=999999}.
         *
         * <p>Mặc định: 100.
         */
        private int maxPageSize = 100;

        public int getMaxPageSize() {
            return maxPageSize;
        }

        public void setMaxPageSize(int maxPageSize) {
            this.maxPageSize = maxPageSize;
        }
    }
}
