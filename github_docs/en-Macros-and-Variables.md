# Macros and Variables

> 🇬🇧 You are reading the English version | 🇩🇪 [Deutsche Version](de-Makros-und-Variablen)

KST4Contest offers a flexible system of text snippets, shortcuts and built-in variables that significantly speed up the chat workflow during contests.

---

## Overview

| Type | Access | Purpose |
|---|---|---|
| **Shortcuts** | Button in the toolbar | Quick text insert into the send field |
| **Snippets** | Right-click / Ctrl+1..0 | Text building blocks, optional PM sending |
| **Variables** | Usable in all text fields | Dynamic values (QRG, locator, AP data) |

---

## Shortcuts (Quick-Access Buttons)

Configurable in Preferences → **Shortcut Settings**.

- Each configured text creates **one button** in the user interface.
- Clicking a button inserts the text into the **send field**.
- **All variables** can be used in shortcuts and are resolved immediately when inserted.
- Longer texts are also possible.

**Tip**: Set up frequently used abbreviations like "pse", "rrr", "tnx", "73" as shortcuts.

---

## Snippets (Text Building Blocks)

Configurable in Preferences → **Snippet Settings**.

### Access

- **Right-click** on a callsign in the user list
- **Right-click** in the CQ message table
- **Right-click** in the PM message table
- **Keyboard shortcuts**: `Ctrl+1` to `Ctrl+0` for the first 10 snippets

### Behaviour with a Selected Callsign

When a callsign is selected in the user list, the snippet is addressed as a **private message**:

```
/CQ CALLSIGN <snippet text>
```

Then **Enter** can be pressed to send directly – even if the send field does not have focus.

### Hardware Macro Keyboard

*(Idea by IU3OAR, Gianluca Costantino)*

The key combinations `Ctrl+1` to `Ctrl+0` can be assigned to a programmable macro keyboard. One key press triggers the snippet, another press (mapped to Enter) sends it immediately. In contest operation this saves considerable time.

### Predefined Default Snippets

On first start, some snippets are pre-configured, e.g.:

- `Hi OM, try sked?`
- `I am calling cq ur dir, pse lsn to me at MYQRG`
- `pse ur qrg?`
- `rrr, I move to your qrg nw, pse ant dir me`

These can be customised or deleted in the Preferences.

---

## Variables

Variables in written texts (snippets, shortcuts, beacon, send field) are replaced by their current values at runtime. Simply type the variable name in **uppercase** in the text.

### MYQRG

Replaced by the current transceiver frequency.

- Source: TRX sync via UDP from the logging software (if enabled)
- Fallback: Manually entered value in the MYQRG text field to the right of the send button
- Format: `144.388.03`

**Example**: `calling cq at MYQRG` → `calling cq at 144.388.03`

### MYQRGSHORT

Like MYQRG, but only the first 7 characters.

- Format: `144.388`

**Example**: `qrg: MYQRGSHORT` → `qrg: 144.388`

### MYLOCATOR

Replaced by your own Maidenhead locator (6 characters).

- Format: `JO51IJ`

**Example**: `my loc: MYLOCATOR` → `my loc: JO51IJ`

### MYLOCATORSHORT

Like MYLOCATOR, but only the first 4 characters.

- Format: `JO51`

**Example**: `loc: MYLOCATORSHORT` → `loc: JO51`

### QRZNAME

Replaced by the **name** of the currently selected station from the chat name field.

**Example**: `Hi QRZNAME, sked?` → `Hi Gianluca, sked?`

### FIRSTAP

Replaced by data of the first reflectable aircraft to the selected station (if available).

- Condition: AirScout is active and an aircraft is available.
- Example format: `a very big AP in 1 min`

**Example**: `AP info: FIRSTAP` → `AP info: a very big AP in 1 min`

### SECONDAP

Like FIRSTAP, but for the second available aircraft.

- Example format: `Next big AP in 9 min`

**Example**: `also: SECONDAP` → `also: Next big AP in 9 min`

### MYQTF *(planned for v1.3)*

Replaced by the current antenna direction in words (e.g. `north`, `north east`, `east`, …).

- Source: Degree value in the MYQTF input field (to the right of the MYQRG field)

---

## Variables in the Beacon

All variables can also be used in the **automatic beacon** (interval messages). Recommended beacon configuration:

```
calling cq at MYQRG, loc MYLOCATOR, GL all!
```

Since KST4Contest automatically reads QRG data from chat messages: if other stations also use KST4Contest, they will immediately see your QRG in the QRG column of their user list.

---

## Example Contest Workflow with Macros

1. Select a station in the user list → callsign is now pre-selected.
2. Press `Ctrl+1` → Snippet "Hi OM, try sked?" is addressed as a PM.
3. Press Enter → Message sent.
4. Station replies with frequency → QRG column is automatically filled.
5. Press `Ctrl+2` → Snippet "I am calling cq ur dir, pse lsn to me at 144.388" (MYQRG resolved).
6. Press Enter → Sent.

No manual typing, no errors, no interruption to CQ calling.
