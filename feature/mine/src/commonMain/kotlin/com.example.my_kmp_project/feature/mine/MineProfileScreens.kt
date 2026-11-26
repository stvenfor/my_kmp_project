package com.example.my_kmp_project.feature.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_kmp_project.core.design.DemoColors
import com.example.my_kmp_project.core.design.MineIcons
import com.example.my_kmp_project.core.design.MineTopBar
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.core.ui.ReportMainTabRoot
import kotlinx.coroutines.launch

@Composable
internal fun ProfileEditScreen(onBack: () -> Unit, onLoggedOut: () -> Unit = {}) {
    val saved = remember { MineProfileLogic.sessionNickname() }
    var name by remember { mutableStateOf(saved) }
    var savedNick by remember { mutableStateOf(saved) }
    var pendingAvatar by remember { mutableStateOf(false) }
    val dirty = MineProfileLogic.isDirty(name, savedNick, pendingAvatar)
    val phone = MineProfileLogic.sessionPhoneMasked()
    val cardShape = RoundedCornerShape(MineTheme.RadiusMd)
    ReportMainTabRoot(isRoot = false)
    Column(
        Modifier
            .fillMaxSize()
            .background(MineTheme.Background)
            .verticalScroll(rememberScrollState()),
    ) {
        MineTopBar(
            title = "个人资料",
            onBack = onBack,
            containerColor = MineTheme.Surface,
            actions = {
                TextButton(
                    onClick = {
                        val err = MineProfileLogic.validateSave(name, dirty)
                        if (err != null) {
                            showPlatformToast(err)
                            return@TextButton
                        }
                        MineProfileLogic.saveNickname(name)
                        savedNick = name.trim()
                        pendingAvatar = false
                        showPlatformToast("资料已保存")
                        onBack()
                    },
                    enabled = dirty,
                ) {
                    Text(
                        "保存",
                        color = if (dirty) MineTheme.Accent else MineTheme.LabelTertiary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                    )
                }
            },
        )
        Spacer(Modifier.height(24.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    Modifier
                        .width(104.dp)
                        .height(104.dp)
                        .clip(RoundedCornerShape(52.dp))
                        .background(MineTheme.FillSecondary)
                        .clickable {
                            pendingAvatar = true
                            showPlatformToast("头像已选择（待上传，平台 gap）")
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = MineIcons.Person,
                        contentDescription = null,
                        tint = MineTheme.LabelTertiary,
                        modifier = Modifier.size(48.dp),
                    )
                }
                Box(
                    Modifier
                        .width(32.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MineTheme.Accent)
                        .border(2.5.dp, DemoColors.OnPrimary, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = MineIcons.Info,
                        contentDescription = null,
                        tint = DemoColors.OnPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("轻触更换头像", color = MineTheme.LabelTertiary, fontSize = 13.sp)
        }
        Spacer(Modifier.height(32.dp))
        Text(
            "基本信息",
            color = MineTheme.LabelTertiary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.4.sp,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(8.dp))
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(cardShape)
                .background(MineTheme.Surface)
                .border(MineTheme.Hairline, MineTheme.Separator, cardShape),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("昵称", fontSize = MineTheme.BodySize, color = MineTheme.LabelPrimary, modifier = Modifier.width(72.dp))
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    textStyle = TextStyle(fontSize = MineTheme.BodySize, color = MineTheme.LabelPrimary, textAlign = TextAlign.End),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            if (name.isEmpty()) {
                                Text("请输入昵称", color = MineTheme.LabelTertiary, fontSize = MineTheme.BodySize)
                            }
                            inner()
                        }
                    },
                )
                Text("›", fontSize = 18.sp, color = MineTheme.LabelTertiary, modifier = Modifier.padding(start = 4.dp))
            }
            HorizontalDivider(
                thickness = MineTheme.Hairline,
                color = MineTheme.Separator,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("手机号", fontSize = MineTheme.BodySize, color = MineTheme.LabelPrimary, modifier = Modifier.width(72.dp))
                Text(
                    phone,
                    fontSize = MineTheme.BodySize,
                    color = MineTheme.LabelSecondary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        Box(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(cardShape)
                .background(MineTheme.Surface)
                .border(MineTheme.Hairline, MineTheme.Separator, cardShape)
                .clickable {
                    MineProfileLogic.logout()
                    showPlatformToast("已退出登录")
                    onLoggedOut()
                }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("退出登录", color = MineTheme.Danger, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
internal fun AddressListScreen(
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (String) -> Unit,
) {
    AddressMockStore.version
    val scope = rememberCoroutineScope()
    // 进入页面即拉取远端地址列表（失败只提示，保留旧缓存）
    LaunchedEffect(Unit) { AddressMockStore.refresh() }
    val list = AddressMockStore.addresses()
    val cardShape = RoundedCornerShape(MineTheme.RadiusMd)
    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(MineTheme.Background)) {
        MineTopBar(
            title = "收货地址",
            onBack = onBack,
            containerColor = MineTheme.Surface,
            actions = {
                TextButton(onClick = onCreate) {
                    Text("新增", color = MineTheme.Accent, fontWeight = FontWeight.SemiBold)
                }
            },
        )
        if (AddressMockStore.loading) {
            Text(
                "加载中…",
                color = MineTheme.LabelTertiary,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        AddressMockStore.errorMessage?.let { message ->
            Text(
                message,
                color = MineTheme.Danger,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        if (list.isEmpty() && !AddressMockStore.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("暂无地址", color = MineTheme.LabelTertiary)
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = onCreate) { Text("添加收货地址", color = MineTheme.Accent) }
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { it.id }) { a ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(MineTheme.Surface)
                            .border(MineTheme.Hairline, MineTheme.Separator, cardShape)
                            .clickable { onEdit(a.id) }
                            .padding(14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${a.receiverName}  ${a.phoneMasked}",
                                fontWeight = FontWeight.Medium,
                                fontSize = MineTheme.BodySize,
                                color = MineTheme.LabelPrimary,
                            )
                            if (a.isDefault) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "默认",
                                    color = MineTheme.Accent,
                                    fontSize = 11.sp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MineTheme.LinkBgSoft)
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(a.line, color = MineTheme.LabelSecondary, fontSize = 13.sp)
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth()) {
                            if (!a.isDefault) {
                                Text(
                                    "设为默认",
                                    color = MineTheme.Accent,
                                    fontSize = 13.sp,
                                    modifier = Modifier
                                        .clickable {
                                            scope.launch {
                                                if (AddressMockStore.setDefault(a.id)) {
                                                    showPlatformToast("已设为默认")
                                                } else {
                                                    showPlatformToast(AddressMockStore.errorMessage ?: "设置失败")
                                                }
                                            }
                                        }
                                        .padding(vertical = 12.dp),
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            Text(
                                "删除",
                                color = MineTheme.LabelTertiary,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .clickable {
                                        scope.launch {
                                            if (AddressMockStore.delete(a.id)) {
                                                showPlatformToast("已删除")
                                            } else {
                                                showPlatformToast(AddressMockStore.errorMessage ?: "删除失败")
                                            }
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private val DemoRegions = listOf(
    Triple("北京市", "北京市", "朝阳区"),
    Triple("北京市", "北京市", "海淀区"),
    Triple("上海市", "上海市", "浦东新区"),
    Triple("广东省", "深圳市", "南山区"),
    Triple("广东省", "广州市", "天河区"),
)

@Composable
internal fun AddressEditScreen(onBack: () -> Unit) {
    val editing = AddressMockStore.find(AddressMockStore.selectedAddressId)
    var name by remember { mutableStateOf(editing?.receiverName.orEmpty()) }
    var phone by remember { mutableStateOf(editing?.receiverPhone.orEmpty()) }
    var province by remember { mutableStateOf(editing?.province ?: "北京市") }
    var city by remember { mutableStateOf(editing?.city ?: "北京市") }
    var district by remember { mutableStateOf(editing?.district ?: "朝阳区") }
    var detail by remember { mutableStateOf(editing?.detailAddress.orEmpty()) }
    var label by remember { mutableStateOf(editing?.label.orEmpty()) }
    var isDefault by remember { mutableStateOf(editing?.isDefault ?: false) }
    var regionPickerOpen by remember { mutableStateOf(false) }
    val cardShape = RoundedCornerShape(MineTheme.RadiusMd)
    val regionLabel = listOf(province, city, district).filter { it.isNotBlank() }.joinToString(" ")
    val scope = rememberCoroutineScope()

    fun save() {
        val err = AddressMockStore.validate(name, phone, province, city, district, detail)
        if (err != null) {
            showPlatformToast(err)
            return
        }
        scope.launch {
            val saved = AddressMockStore.save(
                id = editing?.id,
                name = name,
                phone = phone,
                province = province,
                city = city,
                district = district,
                detail = detail,
                label = label,
                isDefault = isDefault,
            )
            if (saved != null) {
                showPlatformToast("已保存")
                onBack()
            } else {
                showPlatformToast(AddressMockStore.errorMessage ?: "保存失败")
            }
        }
    }

    if (regionPickerOpen) {
        AlertDialog(
            onDismissRequest = { regionPickerOpen = false },
            title = { Text("选择省市区") },
            text = {
                Column {
                    DemoRegions.forEach { (p, c, d) ->
                        val selected = province == p && city == c && district == d
                        Text(
                            "$p $c $d",
                            color = if (selected) MineTheme.Accent else MineTheme.LabelPrimary,
                            fontSize = MineTheme.BodySize,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    province = p
                                    city = c
                                    district = d
                                    regionPickerOpen = false
                                }
                                .padding(vertical = 10.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { regionPickerOpen = false }) { Text("取消") }
            },
        )
    }

    ReportMainTabRoot(isRoot = false)
    Column(Modifier.fillMaxSize().background(MineTheme.Background)) {
        MineTopBar(
            title = if (editing == null) "新增地址" else "编辑地址",
            onBack = onBack,
            containerColor = MineTheme.Surface,
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(MineTheme.Surface)
                    .border(MineTheme.Hairline, MineTheme.Separator, cardShape)
                    .padding(horizontal = 16.dp),
            ) {
                AddressField("收货人", name) { name = it }
                HorizontalDivider(thickness = MineTheme.Hairline, color = MineTheme.Separator)
                AddressField("手机号", phone) { phone = it }
                HorizontalDivider(thickness = MineTheme.Hairline, color = MineTheme.Separator)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { regionPickerOpen = true }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("省市区", fontSize = MineTheme.BodySize, color = MineTheme.LabelPrimary, modifier = Modifier.width(88.dp))
                    Text(
                        regionLabel.ifBlank { "请选择省市区" },
                        fontSize = MineTheme.BodySize,
                        color = if (regionLabel.isBlank()) MineTheme.LabelTertiary else MineTheme.LabelPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Text("›", fontSize = 20.sp, color = MineTheme.LabelTertiary)
                }
                HorizontalDivider(thickness = MineTheme.Hairline, color = MineTheme.Separator)
                AddressField("详细地址", detail, singleLine = false) { detail = it }
                HorizontalDivider(thickness = MineTheme.Hairline, color = MineTheme.Separator)
                AddressField("标签（可选）", label) { label = it }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(MineTheme.Surface)
                    .border(MineTheme.Hairline, MineTheme.Separator, cardShape)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("设为默认地址", fontSize = MineTheme.BodySize, color = MineTheme.LabelPrimary, modifier = Modifier.weight(1f))
                Switch(
                    checked = isDefault,
                    onCheckedChange = { isDefault = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = MineTheme.Accent),
                )
            }
        }
        Text(
            "保存",
            color = DemoColors.OnPrimary,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            fontSize = MineTheme.BodySize,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(MineTheme.Accent)
                .clickable { save() }
                .padding(vertical = 14.dp),
        )
    }
}

@Composable
private fun AddressField(
    label: String,
    value: String,
    singleLine: Boolean = true,
    onChange: (String) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
    ) {
        Text(label, fontSize = MineTheme.BodySize, color = MineTheme.LabelPrimary, modifier = Modifier.width(88.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = singleLine,
            textStyle = TextStyle(fontSize = MineTheme.BodySize, color = MineTheme.LabelPrimary),
            modifier = Modifier
                .weight(1f)
                .then(if (singleLine) Modifier else Modifier.height(64.dp)),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        when (label) {
                            "收货人" -> "请输入收货人"
                            "手机号" -> "请输入手机号"
                            "详细地址" -> "街道、门牌号等"
                            "标签（可选）" -> "家 / 公司"
                            else -> ""
                        },
                        color = MineTheme.LabelTertiary,
                        fontSize = MineTheme.BodySize,
                    )
                }
                inner()
            },
        )
    }
}
