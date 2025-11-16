package fr.leowenex.hashtransfer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "hashtransfer")
public class HashTransferProperties {

    private int expirationMinutes = 60 * 24;
    private String expirationCheckCron = "0 0/15 * * * *";
    private String fileStorageDirectory = "files/";
    private String metadataFileName = "metadata.json";

}
