--[[
  strip-wiki-links.lua – pandoc Lua filter for KST4Contest documentation
  -----------------------------------------------------------------------
  1. Removes language-switch blockquotes (GitHub Wiki navigation) that
     are not relevant in the printed PDF manual.
  2. Converts internal GitHub-wiki-style links to in-document anchors
     so links jump within the generated PDF.
  3. Replaces flag emoji and other symbols that XeLaTeX cannot render with
     plain-text equivalents.
--]]

local PAGE_ANCHOR_MAP = {
  ["de-Home"] = "kst4contest-wiki",
  ["de-Installation"] = "installation",
  ["de-Konfiguration"] = "konfiguration",
  ["de-Funktionen"] = "funktionen",
  ["de-Benutzeroberflaeche"] = "benutzeroberflache",
  ["de-Makros-und-Variablen"] = "makros-und-variablen",
  ["de-Log-Synchronisation"] = "log-synchronisation",
  ["de-AirScout-Integration"] = "airscout-integration",
  ["de-DX-Cluster-Server"] = "integrierter-dx-cluster-server",
  ["de-Changelog"] = "changelog",

  ["en-Home"] = "kst4contest-wiki",
  ["en-Installation"] = "installation",
  ["en-Configuration"] = "configuration",
  ["en-Features"] = "features",
  ["en-User-Interface"] = "user-interface",
  ["en-Macros-and-Variables"] = "macros-and-variables",
  ["en-Log-Sync"] = "log-synchronisation",
  ["en-AirScout-Integration"] = "airscout-integration",
  ["en-DX-Cluster-Server"] = "built-in-dx-cluster-server",
  ["en-Changelog"] = "changelog",

  ["Installation"] = "installation",
  ["Konfiguration"] = "konfiguration",
  ["Funktionen"] = "funktionen",
  ["Benutzeroberflaeche"] = "benutzeroberflache",
  ["Makros-und-Variablen"] = "makros-und-variablen",
  ["Log-Synchronisation"] = "log-synchronisation",
  ["AirScout-Integration"] = "airscout-integration",
  ["DX-Cluster-Server"] = "integrierter-dx-cluster-server",
  ["Changelog"] = "changelog",

  ["Configuration"] = "configuration",
  ["Features"] = "features",
  ["User-Interface"] = "user-interface",
  ["Macros-and-Variables"] = "macros-and-variables",
  ["Log-Sync"] = "log-synchronisation",
}

local function normalize_anchor(text)
  local s = text:lower()
  s = s:gsub("%%20", "-")
  s = s:gsub("ä", "a"):gsub("ö", "o"):gsub("ü", "u"):gsub("ß", "ss")
  s = s:gsub("[^%w%s%-_]", "")
  s = s:gsub("[_%s]+", "-")
  s = s:gsub("%-+", "-")
  s = s:gsub("^%-", ""):gsub("%-$", "")
  return s
end

local function normalize_page_key(page)
  local key = page:gsub("^%./", ""):gsub("^/", "")
  key = key:gsub("^github_docs/", "")
  key = key:gsub("%.md$", "")
  return key
end

local function resolve_page_anchor(page)
  local key = normalize_page_key(page)
  return PAGE_ANCHOR_MAP[key] or normalize_anchor(key)
end

local function convert_url_token(token)
  local url, trailing = token:match("^(https?://%S-)([%.%,%;%:%!%?]?)$")
  if not url then
    return nil
  end

  local link = pandoc.Link({pandoc.Str(url)}, url)
  if trailing ~= "" then
    return {link, pandoc.Str(trailing)}
  end
  return link
end

-- Map of emoji / special Unicode sequences → plain-text replacements.
-- Add more entries here as needed.
local EMOJI_MAP = {
  -- Flag sequences
  ["\xF0\x9F\x87\xAC\xF0\x9F\x87\xA7"] = "[EN]",  -- 🇬🇧
  ["\xF0\x9F\x87\xA9\xF0\x9F\x87\xAA"] = "[DE]",  -- 🇩🇪
  -- Status symbols
  ["\xE2\x9C\x85"] = "[OK]",   -- ✅
  ["\xE2\x9D\x8C"] = "[--]",   -- ❌
  -- Misc symbols used in tables / text
  ["\xF0\x9F\x94\xB4"] = "[red]",    -- 🔴
  ["\xF0\x9F\x9F\xA1"] = "[yellow]", -- 🟡
  ["\xF0\x9F\x9F\xA2"] = "[green]",  -- 🟢
}

--- Replace emoji in a plain string.
local function replace_emoji(text)
  for pattern, replacement in pairs(EMOJI_MAP) do
    text = text:gsub(pattern, replacement)
  end
  return text
end

--- Filter: remove language-switch blockquotes from PDF output.
-- These blockquotes appear in every wiki page for GitHub navigation
-- but are not needed in the printed manual.
function BlockQuote(el)
  local text = pandoc.utils.stringify(el)
  if text:find("Du liest gerade die deutsche Version") or
     text:find("You are reading the English version") then
    return {}
  end
  return el
end

--- Filter: convert internal wiki links to in-PDF anchor links.
function Link(el)
  local target = el.target
  -- Keep external URLs unchanged.
  if target:match("^https?://") or target:match("^mailto:") then
    return el
  end

  if target:match("^#") then
    local fragment = target:gsub("^#", "")
    return pandoc.Link(el.content, "#" .. normalize_anchor(fragment), el.title, el.attr)
  end

  local page, fragment = target:match("^([^#]+)#(.+)$")
  if page and fragment then
    return pandoc.Link(el.content, "#" .. normalize_anchor(fragment), el.title, el.attr)
  end

  return pandoc.Link(el.content, "#" .. resolve_page_anchor(target), el.title, el.attr)
end

--- Filter: replace emoji sequences in plain Str elements.
function Str(el)
  local linkified = convert_url_token(el.text)
  if linkified then
    return linkified
  end

  local replaced = replace_emoji(el.text)
  if replaced ~= el.text then
    return pandoc.Str(replaced)
  end
  return el
end
