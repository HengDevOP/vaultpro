# VaultPro 💰

A modern, high-performance Minecraft economy core engineered for Paper and Purpur servers (**Minecraft 1.21.11 – 26+**). Featuring persistent SQLite storage, automated backup rotation, physical banknotes (Money Paper), an interactive paginated leaderboard GUI with real-time player search, compact number parsing, and full PlaceholderAPI integration.

---

## ✨ Features

- ⚡ **Modern Paper 1.21.11 – 26+ Native**: Built for Java 21+ with native Adventure component rendering, MiniMessage support, and Paper's `AsyncChatEvent` pipeline.
- 🗄️ **Zero-Config SQLite Storage**: Fast, reliable, thread-safe SQLite database (`database/users.db`) with automatic table initialization and query caching.
- 🛡️ **Automated Database Backups**: Scheduled background backups (`backups/users_backup_<timestamp>.db`) with automatic purge retention (`maxBackups`).
- 💵 **Physical Banknotes (Money Paper)**:
  - Players can withdraw digital balance into physical Banknotes (`/v wit <amount>`).
  - Right-click to instantly deposit banknotes into account.
  - Tagged with `PersistentDataContainer` for anti-duplication security.
  - Anvil rename exploit protection (banknotes cannot be altered in anvils).
  - Custom UI audio and map-art sound feedback on withdrawal and deposit.
- 🏆 **Interactive Leaderboard GUI (`/top`, `/baltop`)**:
  - 54-slot paginated menu showing top player heads with formatted balance lore.
  - Built-in search button (Hopper): type any player's username in chat to jump directly to their ranking page and highlight their head with an enchanted glint.
  - Native Paper chat input handling that never leaks search terms into public chat.
- 💸 **Player Payments (`/pay`)**:
  - Transfer funds with compact number shorthand (e.g. `/pay Alex 10k`, `2.5m`, `1b`, `1t`).
  - Custom experience-orb sound effects on transfer.
  - Seamless integration with the `Settings` plugin (respects payment privacy toggles and notification preferences).
- ⚙️ **Comprehensive Admin Management (`/v`)**:
  - Full control to add, remove, set, or withdraw money with formatted feedback in chat and actionbar.
- 🧩 **PlaceholderAPI Support**: Comprehensive placeholders for scoreboards, chat formatters, TAB menus, and leaderboards.
- 🔌 **Developer Service Provider**: Exposes `VaultService` via Bukkit's `ServicesManager` for easy integration with shop and enchantment plugins.

---

## 📋 Commands & Permissions

| Command | Aliases | Permission | Description |
| :--- | :--- | :--- | :--- |
| `/v` or `/v bal` | `/v balance` | None | View your balance (Chat & Actionbar) |
| `/v wit <amount>` | `/v withdraw` | `vaultpro.admin` | Withdraw money into a physical Money Paper |
| `/v add <player> <amount>` | | `vaultpro.admin` | Add money to a player's balance |
| `/v take <player> <amount>` | `/v remove` | `vaultpro.admin` | Take money from a player's balance |
| `/v reset <player> [amount]` | | `vaultpro.admin` | Reset or set a player's balance |
| `/pay <player> <amount>` | | None | Send money to another player |
| `/top [page]` | `/baltop`, `/moneytop` | `vaultpro.top` | Open the interactive balance leaderboard GUI |

### 🔑 Permission Nodes

- `vaultpro.top` - Access `/top` leaderboard GUI (Default: `true`)
- `vaultpro.admin` - Access administrative commands under `/v` (Default: `op`)

---

## 🧩 PlaceholderAPI Placeholders

| Placeholder | Output Example | Description |
| :--- | :--- | :--- |
| `%vaultpro_balance%` | `500.00` / `1.25M` | Player's formatted balance (number only) |
| `%vaultpro_top_me%` | `Steve` | Viewer's username |
| `%vaultpro_top_money%` | `$ 1.25M` | Viewer's balance with color & currency symbol |
| `%vaultpro_top_rank%` | `1` / `N/A` | Viewer's rank on the leaderboard |
| `%vaultpro_top_<N>_player%` | `Alex` | Username of player at leaderboard rank `N` |
| `%vaultpro_top_<N>_money%` | `10.50M` | Formatted balance of player at rank `N` |

---

## ⚙️ Configuration (`config.yml`)

```yaml
# Format type: COMPACT (e.g. $ 500, $ 6.00K) or GROUPED (e.g. $ 6,000)
formatted: COMPACT

prefix:
  symbol: "$"
  colorFomatted: "&a"

default: 500

# Automated database backup configuration
backup:
  enabled: true
  maxBackups: 10
  backupInterval: 21600 # in seconds (6 hours)
  backupDirectory: "backups"
```

---

## 💻 Developer API

Other plugins can hook into VaultPro using Bukkit's standard `ServicesManager`:

```java
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.khmc.vaultpro.service.VaultService;

RegisteredServiceProvider<VaultService> rsp = Bukkit.getServicesManager().getRegistration(VaultService.class);
if (rsp != null) {
    VaultService economy = rsp.getProvider();
    
    // Check balance
    double balance = economy.getBalance(player, "money");
    
    // Deposit or withdraw
    economy.deposit(player, "money", 100.0);
    economy.withdraw(player, "money", 50.0);
    
    // Formatting
    String display = economy.formatCurrency(balance);
}
```

---

## 🛠️ Building from Source

Requires **JDK 21+** and Gradle:

```bash
# Clone the repository
git clone https://github.com/HengDevOP/vaultpro.git
cd vaultpro

# Build the plugin jar
./gradlew build
```

Compiled jar output will be located in `build/libs/VaultPro-1.0-SNAPSHOT.jar`.
