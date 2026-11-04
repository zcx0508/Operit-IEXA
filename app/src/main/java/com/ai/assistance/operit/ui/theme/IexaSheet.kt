package com.ai.assistance.operit.ui.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * IEXA 参考（手机端 app/iexa/ui/components/IexaComponents.kt）新增组件：
 * 设置行 / 新对话胶囊 / 会话列表顶栏 / 模型选择底部弹层（搜索 + 分组 + 选中）。
 * 与 IexaKit.kt（开关、模型 pill、弹层外壳）配合，用于把 Operit 界面统一成 IEXA 观感。
 */

/** 模型项数据。 */
data class IexaModel(
    val name: String,
    val provider: String,
    val group: String = "全部模型",
)

/** 设置列表行：标题 + 可选副标题，8dp 圆角点击容器。 */
@Composable
fun SettingsRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit = {},
) {
    val p = LocalIexaPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = p.primaryText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = p.mutedText,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

/** 「新对话」胶囊按钮：accent 底 + onAccent 文字，全圆角。 */
@Composable
fun NewChatPill(
    text: String = "新对话",
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val p = LocalIexaPalette.current
    Text(
        text = text,
        color = p.onAccent,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(p.accent)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

/** 会话列表页顶栏：汉堡 + 标题 + 右侧「新对话」pill。 */
@Composable
fun IexaTopBar(
    title: String,
    onMenuClick: () -> Unit = {},
    onNewChat: () -> Unit = {},
) {
    val p = LocalIexaPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(p.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IexaRoundedMenuIcon(
            Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onMenuClick() },
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = p.primaryText,
            modifier = Modifier.weight(1f),
        )
        NewChatPill(onClick = onNewChat)
    }
}

/** 单个模型项：圆角头像 + 名称/提供商 + 选中对勾。 */
@Composable
fun IexaModelItem(
    model: IexaModel,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val p = LocalIexaPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(p.accentDark),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = model.name.take(1),
                color = p.onAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(model.name, color = p.primaryText, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(model.provider, color = p.mutedText, fontSize = 12.sp)
        }
        if (selected) {
            Text("✓", color = p.accent, fontSize = 18.sp)
        }
    }
}

/**
 * 模型选择底部弹层：搜索框 + 分组模型列表 + 默认/完成按钮。
 */
@Composable
fun IexaModelSheet(
    visible: Boolean,
    models: List<IexaModel>,
    selectedName: String?,
    onSelect: (IexaModel) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalIexaPalette.current

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onDismiss() },
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(p.surface)
                .navigationBarsPadding(),
        ) {
            Text(
                text = "选择模型",
                color = p.primaryText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            )

            var query by remember { mutableStateOf("") }
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("搜索模型", color = p.mutedText) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = p.surfaceAlt,
                    unfocusedContainerColor = p.surfaceAlt,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = p.accent,
                    focusedTextColor = p.primaryText,
                    unfocusedTextColor = p.primaryText,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
            )

            Spacer(Modifier.height(10.dp))

            val filtered = if (query.isBlank()) {
                models
            } else {
                models.filter {
                    it.name.contains(query, ignoreCase = true) ||
                        it.provider.contains(query, ignoreCase = true)
                }
            }
            val grouped = filtered.groupBy { it.group }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
            ) {
                if (filtered.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            text = "无匹配模型",
                            color = p.mutedText,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                grouped.forEach { (group, list) ->
                    item(key = "g-$group") {
                        Text(
                            text = group,
                            color = p.mutedText,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                        )
                    }
                    items(list, key = { it.name }) { model ->
                        IexaModelItem(
                            model = model,
                            selected = model.name == selectedName,
                            onClick = { onSelect(model) },
                        )
                    }
                }
            }

            Text(
                text = "将本会话绑定到分组以实现自动回退或负载均衡。",
                color = p.mutedText,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    text = "默认",
                    color = p.primaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, p.border, RoundedCornerShape(12.dp))
                        .clickable { onDismiss() }
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "完成",
                    color = p.onAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(p.accent)
                        .clickable { onDismiss() }
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                )
            }
        }
    }
}
