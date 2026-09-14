package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entities.HostelGroupEntity
import com.example.ui.theme.EmeraldPrimary

@Composable
fun GroupManagementDialog(
    currentGroup: HostelGroupEntity?,
    allGroups: List<HostelGroupEntity>,
    onDismiss: () -> Unit,
    onSwitchGroup: (String) -> Unit,
    onCreateGroup: (name: String, creatorName: String) -> Unit,
    onJoinGroup: (code: String, userName: String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Switch/List, 1: Create, 2: Join
    var newHostelName by remember { mutableStateOf("") }
    var creatorName by remember { mutableStateOf("") }
    var joinCode by remember { mutableStateOf("") }
    var joinUserName by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("group_management_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.MeetingRoom, contentDescription = null, tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Hostel Groups", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tabs: Switch, Create, Join
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("My Hostels", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Create", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Join", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (selectedTab) {
                    0 -> {
                        // List & Switch
                        Column(modifier = Modifier.height(240.dp)) {
                            Text(
                                text = "Switch active hostel cash box:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(allGroups) { group ->
                                    val isCurrent = group.groupId == currentGroup?.groupId
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isCurrent) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.surfaceVariant,
                                        border = if (isCurrent) androidx.compose.foundation.BorderStroke(2.dp, EmeraldPrimary) else null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onSwitchGroup(group.groupId)
                                                onDismiss()
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = group.groupName,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Code: ${group.groupCode}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (isCurrent) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Active",
                                                    tint = EmeraldPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // Create New Hostel Form
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Text(text = "Hostel / Flat Name", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = newHostelName,
                                onValueChange = { newHostelName = it },
                                placeholder = { Text("e.g. Green Horizon Mess, Flat 4B") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("create_hostel_name_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(text = "Your Name", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = creatorName,
                                onValueChange = { creatorName = it },
                                placeholder = { Text("e.g. Pial, Shakib") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("create_hostel_creator_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Button(
                                onClick = {
                                    if (newHostelName.isNotBlank() && creatorName.isNotBlank()) {
                                        onCreateGroup(newHostelName.trim(), creatorName.trim())
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("confirm_create_hostel_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                Text("Create & Open Box", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    2 -> {
                        // Join with code
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Text(text = "Hostel Share Code", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = joinCode,
                                onValueChange = { joinCode = it.uppercase() },
                                placeholder = { Text("e.g. HST-XXXXX") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("join_hostel_code_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(text = "Your Name", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = joinUserName,
                                onValueChange = { joinUserName = it },
                                placeholder = { Text("Your name in this mess") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("join_hostel_user_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Button(
                                onClick = {
                                    if (joinCode.isNotBlank() && joinUserName.isNotBlank()) {
                                        onJoinGroup(joinCode.trim(), joinUserName.trim())
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("confirm_join_hostel_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                Text("Join Shared Box", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
