package cn.hubbo.utils;

import cn.hubbo.vo.SecurityVO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Slf4j
public class CipherUtilsUnitTest {


    @DisplayName("协商密钥测试")
    @Test
    public void testGenerateAgreedSecret() {
        SecurityVO res = CipherUtils.computeSharedSecret("+0p0phzfoztVTCmRLXuKWMVcg8OJgB8gE7O4mD0jvGk=");
        log.info("计算出的密钥信息 {}", res);
    }

    @Test
    public void testBase64() {
        String str = Base64.getEncoder().encodeToString("a".getBytes(StandardCharsets.UTF_8));
        log.info("a的base64 {}", str);
    }


}
