package com.example.a3310_project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    val userPreferences = remember { UserPreferences(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var isLoggedIn by remember { mutableStateOf(false) }
    var currentUser by remember { mutableStateOf<Profile?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTicket by remember { mutableStateOf<TicketEntry?>(null) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showPostDialog by remember { mutableStateOf(false) }
    var authErrorMessage by remember { mutableStateOf("") }
    var showLoginPrompt by remember { mutableStateOf(false) }

    // Sample tickets data - now loaded from database
    var allTickets by remember { mutableStateOf<List<TicketEntry>>(emptyList()) }

    // Check if user is logged in when screen loads
    LaunchedEffect(Unit) {
        // Load tickets
        allTickets = dbHelper.getAllTickets()
        
        // Check for saved logged-in user
        userPreferences.loggedInUserIdFlow.collect { userId ->
            if (userId != null) {
                val profile = dbHelper.getProfile(userId)
                if (profile != null) {
                    currentUser = profile
                    isLoggedIn = true
                }
            }
        }
    }

    // Filter tickets based on search query
    val filteredTickets = if (searchQuery.isEmpty()) {
        allTickets
    } else {
        allTickets.filter { ticket ->
            ticket.name.contains(searchQuery, ignoreCase = true) ||
            ticket.description?.contains(searchQuery, ignoreCase = true) == true
        }
    }

    // Show purchase dialog if a ticket is selected (only if logged in)
    selectedTicket?.let { ticket ->
        if (isLoggedIn) {
            PurchaseDialog(
                ticket = ticket,
                onDismiss = { selectedTicket = null },
                onPurchase = {
                    selectedTicket = null
                    showPaymentDialog = true
                }
            )
        }
    }

    // Show payment dialog
    if (showPaymentDialog) {
        PaymentDialog(
            onDismiss = { showPaymentDialog = false },
            onConfirm = {
                showPaymentDialog = false
                // Here you would process the payment
            }
        )
    }

    // Show authentication dialog
    if (showAuthDialog) {
        AuthDialog(
            dbHelper = dbHelper,
            onDismiss = {
                showAuthDialog = false
                authErrorMessage = ""
            },
            onLoginSuccess = { user ->
                currentUser = user
                isLoggedIn = true
                showAuthDialog = false
                authErrorMessage = ""
                // Save logged-in user to preferences
                coroutineScope.launch {
                    userPreferences.saveLoggedInUser(user.userId)
                }
            },
            onError = { message ->
                authErrorMessage = message
            },
            errorMessage = authErrorMessage
        )
    }

    // Show post ticket dialog (only if logged in)
    if (showPostDialog) {
        if (isLoggedIn) {
            PostTicketDialog(
                onDismiss = { showPostDialog = false },
                onPost = { title, description, price ->
                    currentUser?.let { user ->
                        val success = dbHelper.insertTicket(
                            userId = user.userId,
                            name = title,
                            price = price.toString(),
                            description = description
                        )
                        if (success) {
                            // Reload tickets after posting
                            allTickets = dbHelper.getAllTickets()
                        }
                    }
                    showPostDialog = false
                }
            )
        }
    }

    // Show login prompt dialog for non-logged-in users trying to purchase
    if (showLoginPrompt) {
        AlertDialog(
            onDismissRequest = { showLoginPrompt = false },
            title = { Text("Login Required", fontWeight = FontWeight.Bold) },
            text = { Text("You must be logged in to purchase tickets. Would you like to login now?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLoginPrompt = false
                        showAuthDialog = true
                    }
                ) {
                    Text("Login")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLoginPrompt = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search for events, artists...") }
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (!isLoggedIn) {
            // Login Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Welcome to Fanatix",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Buy and sell tickets with fans",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Button(
                        onClick = {
                            showAuthDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Login to Get Started")
                    }
                }
            }
        } else {
            // User Info Card with Post Button
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User avatar
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(60.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = currentUser?.name?.first()?.uppercase() ?: "U",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Welcome back,",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = currentUser?.name ?: "",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { showPostDialog = true }
                        ) {
                            Text("Post Ticket")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Logout button
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                userPreferences.clearLoggedInUser()
                                currentUser = null
                                isLoggedIn = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Logout")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tickets List
        Text(
            text = "Available Tickets",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn {
            items(filteredTickets) { ticket ->
                TicketCard(
                    ticket = ticket,
                    onClick = {
                        if (isLoggedIn) {
                            selectedTicket = ticket
                        } else {
                            showLoginPrompt = true
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun TicketCard(ticket: TicketEntry, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = ticket.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = ticket.description ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Seller: ${ticket.userId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "$${ticket.price}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun PurchaseDialog(
    ticket: TicketEntry,
    onDismiss: () -> Unit,
    onPurchase: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Purchase", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("You are about to purchase:", style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = ticket.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = ticket.description ?: "",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Seller:")
                            Text(
                                ticket.userId,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Price:")
                            Text(
                                "$${ticket.price}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onPurchase
            ) {
                Text("Proceed to Payment")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PaymentDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var cardNumber by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var cardholderName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Payment Details", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text("Enter your payment information", style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(16.dp))

                // Cardholder Name
                OutlinedTextField(
                    value = cardholderName,
                    onValueChange = { cardholderName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Cardholder Name") },
                    placeholder = { Text("John Doe") }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Card Number
                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { if (it.length <= 16) cardNumber = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Card Number") },
                    placeholder = { Text("1234 5678 9012 3456") }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Expiry Date
                    OutlinedTextField(
                        value = expiryDate,
                        onValueChange = { if (it.length <= 5) expiryDate = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Expiry") },
                        placeholder = { Text("MM/YY") }
                    )

                    // CVV
                    OutlinedTextField(
                        value = cvv,
                        onValueChange = { if (it.length <= 3) cvv = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("CVV") },
                        placeholder = { Text("123") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = cardNumber.isNotEmpty() && expiryDate.isNotEmpty() &&
                        cvv.isNotEmpty() && cardholderName.isNotEmpty()
            ) {
                Text("Complete Purchase")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss
            ) {
                Text("Cancel Transaction")
            }
        }
    )
}

@Composable
fun AuthDialog(
    dbHelper: DatabaseHelper,
    onDismiss: () -> Unit,
    onLoginSuccess: (Profile) -> Unit,
    onError: (String) -> Unit,
    errorMessage: String
) {
    var isLoginMode by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isLoginMode) "Login to Fanatix" else "Create Account",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                if (errorMessage.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (!isLoginMode) {
                    // Name field for registration
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Full Name") },
                        placeholder = { Text("Enter your full name") }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Username field
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Username") },
                    placeholder = { Text("Enter your username") }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    placeholder = { Text("Enter your password") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Toggle between login and register
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isLoginMode) "Don't have an account? " else "Already have an account? ",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (isLoginMode) "Register" else "Login",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            isLoginMode = !isLoginMode
                            username = ""
                            password = ""
                            name = ""
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isLoginMode) {
                        // Login logic using database
                        val profile = dbHelper.getProfile(username)
                        if (profile != null && profile.password == password) {
                            onLoginSuccess(profile)
                        } else {
                            onError("Wrong username or password")
                        }
                    } else {
                        // Register logic using database
                        if (name.isNotEmpty()) {
                            val existingProfile = dbHelper.getProfile(username)
                            if (existingProfile != null) {
                                onError("Username already exists")
                            } else {
                                val newProfile = Profile(
                                    name = name,
                                    userId = username,
                                    password = password
                                )
                                val success = dbHelper.insertProfile(newProfile)
                                if (success) {
                                    onLoginSuccess(newProfile)
                                } else {
                                    onError("Failed to create account")
                                }
                            }
                        }
                    }
                },
                enabled = username.isNotEmpty() && password.isNotEmpty() &&
                        (isLoginMode || name.isNotEmpty())
            ) {
                Text(if (isLoginMode) "Login" else "Register")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PostTicketDialog(
    onDismiss: () -> Unit,
    onPost: (String, String, Double) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var priceError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sell Your Ticket", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text("Enter the details of the ticket you want to sell", style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(16.dp))

                // Title field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Event Title *") },
                    placeholder = { Text("e.g., Coldplay Concert") }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Description *") },
                    placeholder = { Text("Describe the event and ticket details") },
                    singleLine = false,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Price field
                OutlinedTextField(
                    value = price,
                    onValueChange = {
                        price = it
                        priceError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Price *") },
                    placeholder = { Text("e.g., 120.00") },
                    isError = priceError
                )

                if (priceError) {
                    Text(
                        text = "Please enter a valid price",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "* Required fields",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val priceValue = price.toDoubleOrNull()
                    if (title.isNotEmpty() && description.isNotEmpty() && priceValue != null && priceValue > 0) {
                        onPost(title, description, priceValue)
                    } else {
                        priceError = true
                    }
                },
                enabled = title.isNotEmpty() && description.isNotEmpty() && price.isNotEmpty()
            ) {
                Text("Post Ticket")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}
