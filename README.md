# 📱 Addiction Control

**Addiction Control** is an Android launcher app that helps users fight smartphone addiction by force-stopping apps that are not in a user-approved list. It encourages mindful usage and digital discipline. Optionally, users can unlock access to all apps via a one-time in-app purchase.

---

## ✨ Features

- 🚫 **App Blocking** — Prevents access to non-approved apps
- 🔐 **Unlock All** — One-time in-app purchase to remove all restrictions
- ⏳ **Time Limit Saving** — Stores control preferences and time limits
- 🧘 **Minimalist Approach** — Encourages reduced screen time

---

## 📦 Project Structure

- `PopupActivity`: Alert dialog when a blocked app is opened
- `MainContainerActivity`: Home screen replacement (launcher)
- `SharedPrefHelper`: Local preferences for control settings
- `BillingClient` Integration: Handles in-app purchase (`unlock_discipline_lock_v2`)

---

## 🛠️ Getting Started

### 📋 Prerequisites

- Android Studio (Arctic Fox or newer)
- Android SDK 24+
- Google Play Console account for testing purchases

### 🚀 Installation

```bash
git clone https://github.com/hk973/Addiction_Control.git
cd addiction-control 
```

### 🤝 Contributions

Feel free to open issues or pull requests. Any feature suggestions, bug reports, or improvements are welcome!
