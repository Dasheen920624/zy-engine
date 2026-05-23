# MedKernel 国密套件与密钥轮换指南

> 版本：1.0 | 适用：v1.0 GA | 日期：2026-05-23
> 等保 2.0 三级合规：通信传输加密 + 密钥管理

## 1. 概述

本文档描述 MedKernel v1.0 GA 的国密（SM2/SM3/SM4）套件配置、密钥轮换策略和兼容模式部署方案，
满足等保 2.0 三级对通信传输加密（8.1.2.2）和密钥管理（8.2.4.9）的要求。

## 2. 国密算法套件

### 2.1 算法映射

| 国际算法 | 国密算法 | 用途 | 性能对比 |
|---------|---------|------|---------|
| RSA 2048 | SM2 | 数字签名、密钥交换 | SM2 密钥更短、签名更快 |
| SHA-256 | SM3 | 哈希摘要 | 性能相当 |
| AES-128 | SM4 | 对称加密 | 性能相当 |

### 2.2 证书体系

| 证书类型 | 算法 | 用途 | 有效期 |
|---------|------|------|--------|
| SM2 签名证书 | SM2 + SM3 | TLS 握手签名 | 1 年 |
| SM2 加密证书 | SM2 + SM3 | 密钥交换 | 1 年 |
| RSA 兼容证书 | RSA 2048 + SHA-256 | 兼容模式 | 1 年 |

## 3. Nginx 国密配置

### 3.1 前置条件

- Nginx 编译时启用 `--with-http_ssl_module`
- 安装国密版 Nginx 模块（推荐：Tengine 或 Nginx + TongSSL/CFCA 模块）
- 获取国密双证书（签名证书 + 加密证书）

### 3.2 国密双证书配置

```nginx
# /etc/nginx/conf.d/medkernel-sm.conf
# 国密双证书 + RSA 兼容双栈

server {
    listen 443 ssl;
    server_name medkernel.hospital.local;

    # ── 国密证书（SM2 双证书） ──
    # 签名证书
    ssl_certificate      /etc/nginx/ssl/sm2_sign.crt;
    ssl_certificate_key  /etc/nginx/ssl/sm2_sign.key;
    # 加密证书
    ssl_enc_certificate   /etc/nginx/ssl/sm2_enc.crt;
    ssl_enc_certificate_key /etc/nginx/ssl/sm2_enc.key;

    # ── RSA 兼容证书 ──
    # 非国密客户端（如旧版浏览器）使用 RSA 证书
    # 需要支持双证书的 Nginx/Tengine 版本
    # ssl_certificate      /etc/nginx/ssl/rsa.crt;
    # ssl_certificate_key  /etc/nginx/ssl/rsa.key;

    # ── TLS 协议与套件 ──
    ssl_protocols TLSv1.2 TLSv1.3;
    # 国密套件优先，兼容 RSA 套件
    ssl_ciphers ECC-SM2-WITH-SM4-SM3:ECDHE-SM2-WITH-SM4-SM3:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers on;

    # ── HSTS ──
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    # ── 反向代理到 MedKernel ──
    location /medkernel/ {
        proxy_pass http://127.0.0.1:18080/medkernel/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 外部健康检查
    location /healthz {
        proxy_pass http://127.0.0.1:18080/medkernel/api/health;
    }
}

# HTTP → HTTPS 重定向
server {
    listen 80;
    server_name medkernel.hospital.local;
    return 301 https://$host$request_uri;
}
```

### 3.3 纯 RSA 模式（过渡方案）

如暂未获取国密证书，可先使用 RSA 证书：

```nginx
server {
    listen 443 ssl;
    server_name medkernel.hospital.local;

    ssl_certificate      /etc/nginx/ssl/rsa.crt;
    ssl_certificate_key  /etc/nginx/ssl/rsa.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers on;

    # ... 其余配置同上
}
```

## 4. 应用层国密配置

### 4.1 SM4 数据加密

MedKernel 使用 SM4 算法加密 HEALTH_DATA 类字段（患者敏感信息）。

配置方式（application.yml）：

```yaml
medkernel:
  security:
    encryption:
      algorithm: SM4
      mode: CBC
      padding: PKCS7Padding
      key-source: ENVIRONMENT  # ENVIRONMENT | HSM | KMS
      key-env-name: MEDKERNEL_SM4_KEY
      iv-env-name: MEDKERNEL_SM4_IV
```

### 4.2 密钥来源

| 来源 | 说明 | 适用场景 |
|------|------|---------|
| `ENVIRONMENT` | 环境变量 | 本地部署 |
| `HSM` | 硬件安全模块 | 高安全要求 |
| `KMS` | 密钥管理服务 | 云部署 |

## 5. 密钥轮换

### 5.1 轮换策略

| 密钥类型 | 轮换周期 | 轮换方式 | 影响范围 |
|---------|---------|---------|---------|
| SM2 签名证书 | 1 年 | 证书更新 + Nginx reload | TLS 连接短暂中断 |
| SM2 加密证书 | 1 年 | 证书更新 + Nginx reload | TLS 连接短暂中断 |
| RSA 兼容证书 | 1 年 | 证书更新 + Nginx reload | TLS 连接短暂中断 |
| SM4 数据加密密钥 | 90 天 | 双密钥过渡 + 应用重启 | 需要数据重加密 |
| JWT 签名密钥 | 30 天 | 双密钥过渡 + 配置更新 | 已签发 Token 在过期前有效 |

### 5.2 SM4 密钥轮换步骤

```
1. 生成新 SM4 密钥
2. 配置双密钥模式（旧密钥解密 + 新密钥加密）
3. 更新环境变量：MEDKERNEL_SM4_KEY_NEW
4. 重启应用
5. 执行数据重加密（后台任务，按批次处理）
6. 重加密完成后，移除旧密钥
7. 更新环境变量：MEDKERNEL_SM4_KEY = 新密钥
8. 重启应用
```

### 5.3 TLS 证书轮换步骤

```
1. 申请新证书（提前 30 天）
2. 将新证书部署到 /etc/nginx/ssl/
3. 测试 Nginx 配置：nginx -t
4. 优雅重载：nginx -s reload
5. 验证新证书生效：openssl s_client -connect medkernel.hospital.local:443
6. 归档旧证书
```

### 5.4 JWT 密钥轮换步骤

```
1. 生成新 JWT 签名密钥
2. 配置双密钥验证（旧密钥验证 + 新密钥签名）
3. 更新环境变量：MEDKERNEL_JWT_SECRET_NEW
4. 重启应用
5. 等待旧 Token 自然过期（默认 24 小时）
6. 移除旧密钥
7. 重启应用
```

## 6. 兼容模式

### 6.1 国密 + RSA 双栈

部署架构：

```
客户端 ──→ Nginx（国密/RSA 双栈）──→ MedKernel
              │
              ├── 国密客户端 → SM2 证书 + SM4 套件
              └── RSA 客户端 → RSA 证书 + AES 套件
```

### 6.2 客户端兼容性

| 客户端类型 | 国密支持 | 兼容方案 |
|-----------|---------|---------|
| 国产浏览器（奇安信/360 企业） | 原生支持 | 直接使用国密套件 |
| Chrome/Firefox | 不支持 | 使用 RSA 兼容套件 |
| Java 应用（Bouncy Castle） | 支持 | 配置 SM2 Provider |
| 移动端（国密 SDK） | 支持 | 集成国密 SDK |

### 6.3 降级策略

| 场景 | 降级方案 | 安全等级 |
|------|---------|---------|
| 国密证书过期 | 自动降级到 RSA | 降级但仍加密 |
| 国密模块故障 | 自动降级到 RSA | 降级但仍加密 |
| 全部证书过期 | 拒绝连接 | 安全优先 |

## 7. 审计与合规

### 7.1 密钥操作审计

所有密钥操作（生成、轮换、撤销）必须记录审计日志：

| 操作 | 审计字段 |
|------|---------|
| 密钥生成 | 操作人、时间、算法、密钥 ID |
| 密钥轮换 | 操作人、时间、旧密钥 ID、新密钥 ID |
| 密钥撤销 | 操作人、时间、密钥 ID、撤销原因 |
| 证书更新 | 操作人、时间、证书序列号、有效期 |

### 7.2 等保合规映射

| 等保控制点 | 本文档覆盖 | 证据 |
|-----------|-----------|------|
| 8.1.2.2 通信传输 | 国密 TLS + RSA 兼容 | Nginx 配置 + 证书 |
| 8.1.4.8 数据保密性 | SM4 数据加密 | application.yml + 代码 |
| 8.2.4.9 密码管理 | 密钥轮换策略 | 本文档 §5 |
| 8.1.4.6 可信执行 | 证书签名验证 | TLS 证书链 |

## 8. 变更记录

| 日期 | 版本 | 变更内容 |
|------|------|---------|
| 2026-05-23 | 1.0 | 初始版本：国密套件配置 + 密钥轮换 + 兼容模式 |
