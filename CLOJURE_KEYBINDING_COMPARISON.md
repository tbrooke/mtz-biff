# Clojure Keybinding Comparison: Neovim (Conjure) vs VSCode (Calva)

## Current State Comparison

| Command | Neovim/Conjure Default | VSCode/Calva Default |
|---------|------------------------|---------------------|
| **Evaluation** |
| Evaluate current form | `,ee` | `Ctrl+Enter` |
| Evaluate top-level form | `,er` | `Alt+Enter` |
| Evaluate & comment | `,ece` | N/A |
| Evaluate file | `,ef` | `Alt+Ctrl+C Enter` |
| Evaluate buffer | `,eb` | N/A |
| Evaluate word | `,ew` | N/A |
| Evaluate & replace | `,e!` | N/A |
| Evaluate selection/motion | `,E` | `Ctrl+Enter` (on selection) |
| **REPL** |
| Open REPL log (split) | `,ls` | N/A (panel) |
| Open REPL log (vsplit) | `,lv` | N/A (panel) |
| Send to REPL | N/A | `Ctrl+Alt+C Ctrl+Alt+E` |
| Send top-level to REPL | N/A | `Ctrl+Alt+C Ctrl+Alt+Space` |
| Switch namespace | N/A | `Alt+Ctrl+C Alt+N` |
| **Testing** |
| Run namespace tests | `,tn` | `Ctrl+Alt+C T` |
| Run all tests | `,ta` | `Ctrl+Alt+C Shift+T` |
| Run current test | `,tc` | `Ctrl+Alt+C Ctrl+Alt+T` |
| Rerun failing tests | N/A | `Ctrl+Alt+C Ctrl+T` |
| **Navigation** |
| Go to definition | `gd` | `F12` or `Ctrl+Click` |
| Look up docs | `K` | `Hover` or `Ctrl+K Ctrl+I` |
| **Other** |
| Toggle pretty print | N/A | `Ctrl+Alt+C P` |
| Expand selection | N/A | `Ctrl+W` (Win/Linux: `Shift+Alt+Right`) |

## Proposed Unified Configuration

Since you're using Spacebar as leader and want consistency, here's a proposed unified setup:

### Unified Keybindings (Space-based)

| Command | Proposed Unified | Neovim Implementation | VSCode Implementation |
|---------|-----------------|----------------------|---------------------|
| **Evaluation (Space m e)** |
| Evaluate current form | `SPC m e e` | `,ee` → `<LocalLeader>ee` | Remap from `Ctrl+Enter` |
| Evaluate top-level | `SPC m e r` | `,er` → `<LocalLeader>er` | Remap from `Alt+Enter` |
| Evaluate file | `SPC m e f` | `,ef` → `<LocalLeader>ef` | Remap from `Alt+Ctrl+C Enter` |
| Evaluate buffer | `SPC m e b` | `,eb` → `<LocalLeader>eb` | Add new binding |
| Evaluate & comment | `SPC m e c` | `,ece` → `<LocalLeader>ece` | Add new binding |
| **REPL (Space m r)** |
| Open REPL log | `SPC m r l` | `,ls` → `<LocalLeader>ls` | Focus REPL panel |
| Send to REPL | `SPC m r s` | Add binding | Remap |
| Switch namespace | `SPC m r n` | Add binding | Remap |
| Connect to REPL | `SPC m r c` | `,cf` | Jack-in command |
| **Testing (Space m t)** |
| Run namespace tests | `SPC m t n` | `,tn` → `<LocalLeader>tn` | Remap |
| Run all tests | `SPC m t a` | `,ta` → `<LocalLeader>ta` | Remap |
| Run current test | `SPC m t c` | `,tc` → `<LocalLeader>tc` | Remap |
| Rerun failing tests | `SPC m t f` | Add binding | Remap |
| **Navigation (Space m g)** |
| Go to definition | `SPC m g d` | `gd` (keep as is) | `F12` (keep as is) |
| Look up docs | `SPC m g k` | `K` (keep as is) | Keep hover |

## Implementation Files

### 1. Neovim Configuration Update

Add to `~/.config/nvim/lua/plugins/clojure-unified.lua`:

```lua
-- Unified Clojure keybindings matching VSCode setup
return {
  {
    "AstroNvim/astrocore",
    opts = {
      mappings = {
        n = {
          -- Unified Clojure mappings under <Leader>m
          ["<Leader>m"] = { name = "Major Mode (Clojure)" },
          
          -- Evaluation submenu
          ["<Leader>me"] = { name = "Evaluate" },
          ["<Leader>mee"] = { "<LocalLeader>ee", desc = "Evaluate form" },
          ["<Leader>mer"] = { "<LocalLeader>er", desc = "Evaluate root/top-level" },
          ["<Leader>mef"] = { "<LocalLeader>ef", desc = "Evaluate file" },
          ["<Leader>meb"] = { "<LocalLeader>eb", desc = "Evaluate buffer" },
          ["<Leader>mec"] = { "<LocalLeader>ece", desc = "Evaluate & comment" },
          
          -- REPL submenu
          ["<Leader>mr"] = { name = "REPL" },
          ["<Leader>mrl"] = { "<LocalLeader>ls", desc = "Show REPL log" },
          ["<Leader>mrn"] = { "<cmd>ConjureEvalBuf (in-ns 'current-ns)<cr>", desc = "Switch namespace" },
          
          -- Testing submenu
          ["<Leader>mt"] = { name = "Test" },
          ["<Leader>mtn"] = { "<LocalLeader>tn", desc = "Test namespace" },
          ["<Leader>mta"] = { "<LocalLeader>ta", desc = "Test all" },
          ["<Leader>mtc"] = { "<LocalLeader>tc", desc = "Test current" },
          
          -- Keep LocalLeader mappings for quick access
          ["<LocalLeader>"] = { name = "Clojure" },
        },
      },
    },
  },
}
```

### 2. VSCode Configuration Update

Add to `~/Library/Application Support/Code/User/keybindings.json`:

```json
[
  // Unified Clojure/Calva keybindings
  // Evaluation
  {
    "key": "space m e e",
    "command": "calva.evaluateCurrentForm",
    "when": "calva:activated && editorTextFocus && vim.active && vim.mode == 'Normal'"
  },
  {
    "key": "space m e r",
    "command": "calva.evaluateCurrentTopLevelForm",
    "when": "calva:activated && editorTextFocus && vim.active && vim.mode == 'Normal'"
  },
  {
    "key": "space m e f",
    "command": "calva.loadFile",
    "when": "calva:activated && editorTextFocus && vim.active && vim.mode == 'Normal'"
  },
  
  // REPL
  {
    "key": "space m r l",
    "command": "calva.showOutputWindow",
    "when": "calva:activated && vim.active && vim.mode == 'Normal'"
  },
  {
    "key": "space m r s",
    "command": "calva.sendCurrentFormToOutputWindow",
    "when": "calva:activated && editorTextFocus && vim.active && vim.mode == 'Normal'"
  },
  {
    "key": "space m r n",
    "command": "calva.setOutputWindowNamespace",
    "when": "calva:activated && vim.active && vim.mode == 'Normal'"
  },
  
  // Testing
  {
    "key": "space m t n",
    "command": "calva.runNamespaceTests",
    "when": "calva:activated && vim.active && vim.mode == 'Normal'"
  },
  {
    "key": "space m t a",
    "command": "calva.runAllTests",
    "when": "calva:activated && vim.active && vim.mode == 'Normal'"
  },
  {
    "key": "space m t c",
    "command": "calva.runTestUnderCursor",
    "when": "calva:activated && vim.active && vim.mode == 'Normal'"
  },
  {
    "key": "space m t f",
    "command": "calva.rerunTests",
    "when": "calva:activated && vim.active && vim.mode == 'Normal'"
  },
  
  // Keep quick access with comma (LocalLeader equivalent)
  {
    "key": ", e e",
    "command": "calva.evaluateCurrentForm",
    "when": "calva:activated && editorTextFocus && vim.active && vim.mode == 'Normal'"
  },
  {
    "key": ", e r",
    "command": "calva.evaluateCurrentTopLevelForm", 
    "when": "calva:activated && editorTextFocus && vim.active && vim.mode == 'Normal'"
  }
]
```

## Notes

1. **LocalLeader**: In Neovim, LocalLeader is typically `,` for filetype-specific commands
2. **Space Leader**: Using `SPC m` prefix creates a consistent "major mode" menu like Spacemacs/Doom
3. **Muscle Memory**: Keep both the original shortcuts and new unified ones during transition
4. **VSCode Vim**: Requires VSCodeVim extension with proper leader key configuration

## VSCode Settings for Vim

Ensure your VSCode settings.json includes:

```json
{
  "vim.leader": "<space>",
  "vim.normalModeKeyBindingsNonRecursive": [
    {
      "before": ["<space>"],
      "commands": ["vspacecode.space"]
    }
  ]
}
```