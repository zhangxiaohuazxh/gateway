package cn.hubbo.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class SecurityVO {


    private String clientPubKey;

    private String serverPubKey;

    @JsonIgnore
    private String secret;


}
