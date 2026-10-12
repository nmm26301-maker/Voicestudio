# GPU 加速接口

1.5.8 新增“GPU 加速接口”，使用已部署的 OmniVoice 兼容 `/v1/audio/speech` 服务。手机发送完整文本，服务器常驻模型并用 GPU 推理，手机接收 WAV 后检查文件完整性再保存作品。页面路径：音色 → GPU 加速接口，或设置 → GPU 加速接口。

这项功能需要一个实际可用的 GPU 服务地址。开源代码免费获取不等于有免费的 GPU 服务器；本次没有部署公网服务，也没有用现成账号或密钥调用第三方。

## 开源实现与本项目配置

- [OmniVoice](https://github.com/k2-fsa/OmniVoice)：官方模型与生成算法。本轮按其参考提示和短句时长规则修复 Android 合成。
- [omnivoice-triton](https://github.com/newgrit1004/omnivoice-triton)：NVIDIA GPU 的 Triton 内核融合和 CUDA Graph 等实现。作者的 3.4 倍结果是特定 GPU 测量，不能作为手机或本接口的性能承诺。
- [OmniVoice-Triton-API](https://github.com/hsiang-han/OmniVoice-Triton-API)：可对接的 API。`server/docker-compose.yml` 固定 API 源码提交 `1ffadfd44b81bc6ee8f156bff493c13e55e9e7fb`，设置 `RUNNER_MODE=triton`；不默认使用该项目记录有显存泄漏的 hybrid/faster 模式。底层 pip 依赖仍遵循其 Dockerfile，完整依赖未锁定，生产部署应另外固定镜像与依赖。

## 有 NVIDIA GPU 的电脑/服务器

需要可用的 NVIDIA 驱动、Docker 与 NVIDIA Container Toolkit。本仓库的 Compose 提供 GPU 后端和 Caddy HTTPS 入口。准备一个指向这台服务器的域名，80/443 端口需可达，以申请 HTTPS 证书。

```bash
cd server
cp .env.example .env
# 编辑 .env：填入自己的域名和随机接口密钥；不要提交此文件。
docker compose up -d --build
docker compose logs -f omnivoice
```

首次加载还需下载模型，GPU 显存与耗时需按自己的硬件测量。没有 GPU/域名时，不执行这套部署；也可以直接填入你已有的兼容 HTTPS 服务。

手机填写：

| 配置 | 内容 |
| --- | --- |
| 地址 | `https://你的域名/v1`（也接受完整 `/v1/audio/speech` 地址） |
| 模型 | `k2-fsa/OmniVoice`，或按服务器配置填写；留空使用服务端默认 |
| 音色 | `auto`、`female_zh`、`male_zh` 等服务器支持的名称 |
| API Key | 与 `.env` 的 `API_TOKEN` 一致；现有服务无需认证时可留空 |

点击“保存并使用加速接口”，回到创作页输入文字生成。接口单独默认 32 步，可在创作页选择 8/16/32 步；本机原有步数配置保持不变。密钥与 Fish 密钥分开加密保存，任务文件不存密钥；改变服务地址后，旧的未完成任务需按新配置重新创建，避免把新密钥发送到旧地址。

## 功能边界

当前只在普通文字创作中明确选择该接口，批量、有声书、字幕配音继续按原本本机流程处理。不会自动把文字、录音或参考音色上传，也不会在本机出错时自动切换服务器。

请求包含完整 `input`、`voice`、WAV 格式及语言/步数/语速/种子。固定版本的上游 API **尚未应用 speed，且不处理客户端 seed**，这些字段是否生效取决于所部署的服务；本项目不把“已发送”宣称为“服务端已执行”。本机参考音色不自动转换成服务器音色；若需服务器声音克隆，应在服务器另行配置对应音色。

返回错误、空文件、损坏/截断 WAV、长文本对应极短音频或取消传输不会注册为成功作品。文件和时长检查不能证明每个字都读全，语义完整性仍需试听或转写核对。取消会断开客户端请求；服务器内已经运行的计算能否停止由该服务决定。上线前应在自己的服务器对中文长短句、取消与并发进行验收。
