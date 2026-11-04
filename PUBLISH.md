# Operit-IEXA 推送说明

本目录已重建为独立 Git 仓库（分支 `iexa-visual`，单次初始提交），并非从上游 fork。

- 上游参考：`AAswordman/Operit`（原 remote 指向 `https://gitclone.com/github.com/AAswordman/Operit.git`，已丢弃重建）
- 许可证：LGPL-3.0（见本仓库 `LICENSE`）
- 离线备份：`D:\Operit-IEXA.bundle`（`git bundle create --all`）

---

## 第一步：在 GitHub 网页新建空仓库

1. 打开 <https://github.com/new>
2. Repository name 填 **`Operit-IEXA`**
3. 可见性选 **Public**
4. **不要**勾选 "Add a README file"、"Add .gitignore"、"Choose a license"
   （仓库必须完全为空，否则 push 会和非相关历史的初始提交冲突）
5. 点 Create repository

---

## 第二步：推送（两条路，选一条）

### 路线 A：用 Personal Access Token（可脚本化）

Token 在 <https://github.com/settings/tokens> 生成，勾选 `repo` 权限。

```bash
cd D:/operit-src/repo
git remote add origin https://<USER>:<TOKEN>@github.com/<USER>/Operit-IEXA.git
git push -u origin iexa-visual
```

注意把 `<USER>` 换成你的 GitHub 用户名、`<TOKEN>` 换成刚生成的 token。

> 若已经添加过 origin，先 `git remote set-url origin https://<USER>:<TOKEN>@github.com/<USER>/Operit-IEXA.git`

### 路线 B：在本机终端手动执行（假设已登录 GitHub）

```bash
cd D:/operit-src/repo
git remote add origin https://github.com/<USER>/Operit-IEXA.git
git push -u origin iexa-visual
```

Git Credential Manager 会弹出浏览器登录，无需手输 token。

---

## 第三步：检查结果

```bash
git remote -v
git log --oneline -1
git branch -vv
```

推送成功后，在 GitHub 仓库 Settings → Branches 把默认分支设为 `iexa-visual`（或按需建 `main`）。

---

## 安全提醒（务必阅读）

- **token 用完立即撤销**：推送完成后到 <https://github.com/settings/tokens> 删除该 token。
  带 token 的 remote URL 会明文保存在 `.git/config`，泄露即等于泄露账号写权限。
  撤销后可改为 `git remote set-url origin https://github.com/<USER>/Operit-IEXA.git` 清理明文。
- **不要提交 `local.properties`**：该文件含发布签名密钥库路径与**明文口令**
  （`RELEASE_STORE_PASSWORD`、`RELEASE_KEY_PASSWORD`）。它已被 `.gitignore` 覆盖，
  请勿用 `git add -f` 强行加入。需要的开发者从 `local.properties.example` 复制后自行填写。
- **不要提交签名文件**：`*.keystore` / `*.jks` / `*.p12` / `*.lineage` 均已在 `.gitignore` 中排除。
- **不要提交构建产物**：`build/`、`.gradle/`、`.idea/`、`.kotlin/`、`.cxx/`、`node_modules/` 均已排除。
- 提交前建议自查：`git status --short` 应为空，`git ls-files | grep -E 'local.properties|\.jks|\.keystore'` 应无输出。

---

## 备注

- 仓库体积约 232 MB / 3289 个文件；单个最大文件为
  `terminal/src/main/assets/ubuntu-noble-aarch64-pd-v4.18.0.tar.xz`（约 61 MB），
  低于 GitHub 的 100 MB 单文件硬限制，可直接 push（无需 Git LFS）。
- `.gitmodules` 仍引用上游子模块地址（`terminal`、`tools/hotbuild/OperitNightlyRelease`），
  若需要完全独立，后续可另行替换或移除。
