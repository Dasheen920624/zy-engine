# SM_CRYPTO_SUITE 国密套件与密钥轮换文档

> 版本：1.0 · 2026-05-24
> 任务：GA-SEC-02
> 状态：DONE

## 1. 国密算法标准

| 算法 | 标准 | 用途 | 密钥长度 |
|------|------|------|----------|
| SM2 | GB/T 32918-2017 | 非对称加密/签名 | 256 bit 椭圆曲线 sm2p256v1 |
| SM3 | GB/T 32905-2016 | 杂凑/摘要 | 256 bit 输出 |
| SM4 | GB/T 32907-2016 | 对称加密 | 128 bit，CBC/ECB 模式 |

## 2. 实现架构

```
com.medkernel.common.crypto/
├── SmCryptoService.java      # SM2/SM3/SM4 统一服务（纯函数 stateless）
├── SmCryptoConfig.java       # BouncyCastle Provider 注册
├── SmCryptoException.java    # 统一异常
├── SmKeyPair.java            # SM2 密钥对封装
├── CryptoMode.java           # 国密/国际/双栈模式枚举
├── DualStackCryptoService.java  # 双栈加密门面
└── JceUnlimitedStrengthEnabler.java  # JCE 无限制强度策略

com.medkernel.security/
├── KeyRotationService.java   # 密钥轮换服务
└── EncryptionKey.java        # 加密密钥实体
```

## 3. 加密模式

| 模式 | 代码 | 说明 | 适用场景 |
|------|------|------|----------|
| 纯国密 | `SM_ONLY` | SM2+SM3+SM4 | 等保三级/国密合规（默认） |
| 纯国际 | `INTERNATIONAL_ONLY` | RSA+SHA-256+AES-256-GCM | 海外部署 |
| 双栈兼容 | `DUAL_STACK` | 国密为主，国际为备 | 过渡期/混合环境 |

## 4. 密钥轮换机制

### 4.1 密钥生命周期

```
ACTIVE → GRACE → RETIRED
   ↓
REVOKED（紧急撤销）
```

| 状态 | 说明 | 有效期 |
|------|------|--------|
| ACTIVE | 当前活跃密钥，用于加密/验签 | 无限期（直到轮换） |
| GRACE | 宽限期密钥，仅用于解密/验签 | 默认 24 小时 |
| RETIRED | 已退役密钥，不可使用 | 永久 |
| REVOKED | 紧急撤销密钥，立即失效 | 永久 |

### 4.2 轮换操作

```java
// 执行密钥轮换
KeyVersion newKey = keyRotationService.rotateKey("key-v2", "new-key-material", "admin");

// 紧急撤销
keyRotationService.revokeKey(keyId, "admin");

// 退役过期宽限期密钥
List<KeyVersion> retired = keyRotationService.retireExpiredGraceKeys();
```

### 4.3 JWT 密钥版本

JWT Token 中嵌入密钥版本号（`kid` 字段），验证时根据版本号选择对应密钥：

```json
{
  "sub": "user123",
  "kid": 2,
  "exp": 1716422400
}
```

## 5. 字段加密

### 5.1 透明字段加密

`FieldEncryptionService` 使用 SM4-CBC 透明加密标注了 `@Encrypted` 的字段：

```java
@DataClass(DataClassification.HEALTH_DATA)
public class PatientRecord {
    @Encrypted(maskPolicy = MaskPolicy.ID_CARD)
    private String idCard;  // 自动加密/脱敏
}
```

### 5.2 密文格式

```
SM4:v1:<Base64(IV+cipherText)>
```

- `SM4` — 算法标识
- `v1` — 密钥版本号
- `Base64(IV+cipherText)` — IV(16字节) + 密文

### 5.3 表级派生密钥

每张表使用 SM3 派生的独立密钥，避免跨表密钥泄露：

```java
byte[] tableKey = sm3(masterKey + tableName);
```

## 6. 数据脱敏策略

| 策略 | 原始 | 脱敏后 | 适用字段 |
|------|------|--------|----------|
| ID_CARD | 310101199001011234 | 3101****1234 | 身份证号 |
| PHONE | 13812345678 | 138****5678 | 手机号 |
| EMAIL | zhang@example.com | z***@example.com | 邮箱 |
| NAME | 张三丰 | 张*丰 | 姓名 |
| ADDRESS | 上海市浦东新区... | 上海市浦*** | 地址 |
| FULL | 任意 | ****** | 全掩码 |

## 7. 兼容模式说明

### 7.1 国密/国际双栈

`DualStackCryptoService` 提供统一加密接口，根据 `CryptoMode` 自动选择算法：

- **SM_ONLY**（默认）：所有操作走国密算法
- **DUAL_STACK**：国密优先，降级到国际算法（预留）
- **INTERNATIONAL_ONLY**：纯国际算法（预留）

### 7.2 密钥轮换兼容

轮换期间，新旧密钥共存：
1. 新数据使用新密钥加密
2. 旧数据使用旧密钥解密（宽限期内）
3. 宽限期过后，旧密钥退役
4. 可选：后台任务重新加密旧数据

### 7.3 部署要求

| 组件 | 国密要求 | 说明 |
|------|----------|------|
| JDK | 8u151+ | JCE 无限制强度策略 |
| BouncyCastle | 1.70+ | SM2/SM3/SM4 Provider |
| Nginx | 国密版 | 国密 TLS（可选，需编译 OpenSSL+Nghttp2 国密版） |
| HSM | 国密认证 | 硬件密钥保护（生产推荐） |

## 8. 安全基线

| 项目 | 配置 | 标准 |
|------|------|------|
| JWT 签名 | HS256 | 等保三级 |
| 密码哈希 | BCrypt | OWASP |
| 传输加密 | TLS 1.2+ | GB/T 22239 |
| 字段加密 | SM4-CBC | GB/T 32907 |
| 审计签名 | SM3 | GB/T 32905 |
| 密钥轮换 | 24h 宽限期 | 最佳实践 |
| HSTS | max-age=31536000 | RFC 6797 |

## 9. 修订记录

| 版本 | 日期 | 修改人 | 修改内容 |
|------|------|--------|----------|
| v1.0 | 2026-05-24 | TraeAI-GLM5 | GA-SEC-02：国密套件与密钥轮换文档 |
