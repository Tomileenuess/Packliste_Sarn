/**
 * ============================================================================
 * GOOGLE APPS SCRIPT BACKEND: FAMILIEN-PACKLISTE SARN (REST API)
 * ============================================================================
 * 
 * Endpunkte:
 * - doGet(e): Liest "Packliste" und "Katalog" und gibt strukturiertes JSON zurück.
 * - doPost(e): Speichert Packlisten-Änderungen atomar mit LockService und
 *              trägt neue Artikel automatisch in den "Katalog" ein.
 */

// Tabellenblatt-Namen
const SHEET_PACKLISTE = "Packliste";
const SHEET_KATALOG = "Katalog";

// Standard-Spaltenköpfe
const HEADERS_PACKLISTE = [
  "ID", 
  "Richtung", 
  "Artikel", 
  "Person", 
  "Status", 
  "Kategorie", 
  "Menge", 
  "Letzte_Aenderung"
];

const HEADERS_KATALOG = [
  "Artikel", 
  "Standard_Kategorie", 
  "Standard_Person"
];

// Initialer Grundkatalog (falls das Tabellenblatt neu angelegt wird)
const DEFAULT_CATALOG_ITEMS = [
  // Haushalt / Textilien & Bettwäsche
  ["Frottetuch Hand", "Textilien & Bettwäsche", "Haushalt"],
  ["Frottetuch Duschen", "Textilien & Bettwäsche", "Haushalt"],
  ["Waschlappen", "Textilien & Bettwäsche", "Haushalt"],
  ["Fusstuch Dusche", "Textilien & Bettwäsche", "Haushalt"],
  ["Fixleintücher 90", "Textilien & Bettwäsche", "Haushalt"],
  ["Fixleintücher 160", "Textilien & Bettwäsche", "Haushalt"],
  ["Bettzeug Kids", "Textilien & Bettwäsche", "Haushalt"],
  ["Bettzeug Eltern", "Textilien & Bettwäsche", "Haushalt"],
  ["Bettzeug Gäste", "Textilien & Bettwäsche", "Haushalt"],
  // Haushalt / Küche & Verbrauch
  ["Küche Lumpen", "Küche & Verbrauch", "Haushalt"],
  ["Küche Tüechli", "Küche & Verbrauch", "Haushalt"],
  ["Kleenex", "Küche & Verbrauch", "Haushalt"],
  ["Haushaltspapier", "Küche & Verbrauch", "Haushalt"],
  ["Bodenmopp", "Küche & Verbrauch", "Haushalt"],
  ["Holz", "Küche & Verbrauch", "Haushalt"],
  ["Sonnencreme", "Küche & Verbrauch", "Haushalt"],
  ["neue Packliste", "Küche & Verbrauch", "Haushalt"],
  // Bekleidung & Wäsche (Allgemein)
  ["Pijama", "Bekleidung & Wäsche", "Haushalt"],
  ["Unterhosen", "Bekleidung & Wäsche", "Haushalt"],
  ["Singlet", "Bekleidung & Wäsche", "Haushalt"],
  ["Socken", "Bekleidung & Wäsche", "Haushalt"],
  ["Jeans", "Bekleidung & Wäsche", "Haushalt"],
  ["Hosen", "Bekleidung & Wäsche", "Haushalt"],
  ["Pullover", "Bekleidung & Wäsche", "Haushalt"],
  ["T-Shirt kurz", "Bekleidung & Wäsche", "Haushalt"],
  ["T-shirt lang", "Bekleidung & Wäsche", "Haushalt"],
  ["Trainerhosen", "Bekleidung & Wäsche", "Haushalt"],
  ["Hoodie", "Bekleidung & Wäsche", "Haushalt"],
  ["Shorts", "Bekleidung & Wäsche", "Haushalt"],
  ["Fleece", "Bekleidung & Wäsche", "Haushalt"],
  // Sport & Outdoor (Sommer & Winter)
  ["Badehose", "Sport & Outdoor", "Haushalt"],
  ["Bikini", "Sport & Outdoor", "Bettina"],
  ["Bikehose", "Sport & Outdoor", "Haushalt"],
  ["Rashy", "Sport & Outdoor", "Haushalt"],
  ["Hut", "Sport & Outdoor", "Haushalt"],
  ["Wanderhose", "Sport & Outdoor", "Haushalt"],
  ["Wandersocken", "Sport & Outdoor", "Haushalt"],
  ["Skisocken", "Sport & Outdoor", "Haushalt"],
  ["Skijacke", "Sport & Outdoor", "Haushalt"],
  ["Skihosen", "Sport & Outdoor", "Haushalt"],
  ["Helmstrumpf", "Sport & Outdoor", "Haushalt"],
  ["Thermo Oberteil", "Sport & Outdoor", "Haushalt"],
  ["Thermo Hosen", "Sport & Outdoor", "Haushalt"],
  ["Kappe", "Sport & Outdoor", "Haushalt"],
  ["Merino kurz", "Sport & Outdoor", "Haushalt"],
  ["Merino lang", "Sport & Outdoor", "Haushalt"],
  ["Lange Unterhosen", "Sport & Outdoor", "Haushalt"],
  ["BH", "Sport & Outdoor", "Bettina"],
  // Feste Retour-Artikel (für "Nach Hause")
  ["Kompost", "Retour-Artikel", "Haushalt"],
  ["Brille", "Retour-Artikel", "Tomi"],
  ["Kuscheltierli", "Retour-Artikel", "Luke"],
  ["Medis", "Retour-Artikel", "Haushalt"],
  ["Wanderschuhe", "Retour-Artikel", "Haushalt"],
  ["Windjacken", "Retour-Artikel", "Haushalt"],
  ["Keens", "Retour-Artikel", "Haushalt"],
  ["Winterschuhe", "Retour-Artikel", "Haushalt"],
  ["Schlittschuhe", "Retour-Artikel", "Haushalt"],
  ["Spange Lionel", "Retour-Artikel", "Lionel"]
];

/**
 * ----------------------------------------------------------------------------
 * 1. GET-HANDLER: doGet(e)
 * ----------------------------------------------------------------------------
 * Liefert den aktuellen Datenstand beider Tabellenblätter als JSON zurück.
 */
function doGet(e) {
  try {
    const ss = SpreadsheetApp.getActiveSpreadsheet();
    ensureStructure(ss);

    const sheetPackliste = ss.getSheetByName(SHEET_PACKLISTE);
    const sheetKatalog = ss.getSheetByName(SHEET_KATALOG);

    // 1. Packliste auslesen
    const packlisteRows = readSheetAsObjects(sheetPackliste);

    // Aufteilung in "nach_sarn" und "nach_hause" für die Web-App
    const nach_sarn = [];
    const nach_hause = [];

    packlisteRows.forEach(row => {
      const item = {
        id: String(row["ID"] || ("item_" + Date.now())),
        name: String(row["Artikel"] || ""),
        person: String(row["Person"] || "haushalt").toLowerCase(),
        packed: String(row["Status"]).toLowerCase() === "packed",
        category: String(row["Kategorie"] || ""),
        qty: parseInt(row["Menge"] || 1, 10),
        lastModified: String(row["Letzte_Aenderung"] || "")
      };

      const direction = String(row["Richtung"] || "").toLowerCase();
      if (direction === "sarn" || direction === "nach_sarn") {
        nach_sarn.push(item);
      } else {
        nach_hause.push(item);
      }
    });

    // 2. Katalog auslesen
    const katalogRows = readSheetAsObjects(sheetKatalog);
    const catalog = katalogRows.map(row => ({
      name: String(row["Artikel"] || ""),
      category: String(row["Standard_Kategorie"] || "Allgemein"),
      defaultPerson: String(row["Standard_Person"] || "haushalt").toLowerCase()
    }));

    const responsePayload = {
      status: "success",
      timestamp: new Date().toISOString(),
      active_items: packlisteRows,
      catalog: catalog,
      nach_sarn: nach_sarn,
      nach_hause: nach_hause
    };

    return createJsonResponse(responsePayload);

  } catch (error) {
    return createJsonResponse({
      status: "error",
      message: error.toString()
    });
  }
}

/**
 * ----------------------------------------------------------------------------
 * 2. POST-HANDLER: doPost(e)
 * ----------------------------------------------------------------------------
 * Schreibt Packlisten-Änderungen atomar mit Concurrency-Locking und
 * aktualisiert bei Bedarf den Katalog.
 */
function doPost(e) {
  // Lock gegen gleichzeitige Schreibzugriffe einholen (max. 10 Sekunden warten)
  const lock = LockService.getScriptLock();
  const hasLock = lock.tryLock(10000);

  if (!hasLock) {
    return createJsonResponse({
      status: "error",
      message: "Server beschäftigt: Schreibsperre konnte nicht erlangt werden (Concurrency Lock)."
    });
  }

  try {
    const ss = SpreadsheetApp.getActiveSpreadsheet();
    ensureStructure(ss);

    let requestData;
    if (e && e.postData && e.postData.contents) {
      requestData = JSON.parse(e.postData.contents);
    } else if (e && e.parameter && e.parameter.data) {
      requestData = JSON.parse(e.parameter.data);
    } else {
      throw new Error("Keine Nutzdaten (Payload) im POST-Request empfangen.");
    }

    // Payload kann direkt das Objekt { nach_sarn: [...], nach_hause: [...] } sein
    // oder geschachtelt unter payload: { ... }
    const payload = requestData.payload || requestData;

    const sheetPackliste = ss.getSheetByName(SHEET_PACKLISTE);
    const sheetKatalog = ss.getSheetByName(SHEET_KATALOG);

    // Vorhandenen Katalog zur Dubletten-Prüfung laden
    const existingCatalogItems = getExistingCatalogItemNames(sheetKatalog);
    const newCatalogEntries = [];

    const nowIso = new Date().toISOString();
    const rowsToWrite = [];

    // Hilfsfunktion: Items einer Richtung in Zeilen umwandeln
    function processItemList(items, richtungCode) {
      if (!Array.isArray(items)) return;

      items.forEach(item => {
        const itemName = String(item.name || "").trim();
        if (!itemName) return;

        const id = String(item.id || "item_" + Math.random().toString(36).substr(2, 6));
        const person = capitalizeFirst(String(item.person || "haushalt"));
        const status = item.packed ? "packed" : "open";
        const category = String(item.category || deriveCategory(itemName));
        const qty = parseInt(item.qty || 1, 10);

        rowsToWrite.push([
          id,
          richtungCode, // "sarn" oder "home"
          itemName,
          person,
          status,
          category,
          qty,
          nowIso
        ]);

        // Prüfen ob Artikel im Katalog fehlt -> automatisch anfügen
        const lowerName = itemName.toLowerCase();
        if (!existingCatalogItems.has(lowerName)) {
          existingCatalogItems.add(lowerName);
          newCatalogEntries.push([
            itemName,
            category,
            person
          ]);
        }
      });
    }

    // Richtung 1: Nach Sarn
    if (payload.nach_sarn) {
      processItemList(payload.nach_sarn, "sarn");
    }

    // Richtung 2: Nach Hause
    if (payload.nach_hause) {
      processItemList(payload.nach_hause, "home");
    }

    // Falls flat array als "items" oder "active_items" übergeben wurde
    if (Array.isArray(payload.items) || Array.isArray(payload.active_items)) {
      const flatList = payload.items || payload.active_items;
      flatList.forEach(item => {
        const dir = (item.Richtung || item.direction || "sarn").toLowerCase().includes("hause") || 
                    (item.Richtung || item.direction || "").toLowerCase() === "home" ? "home" : "sarn";
        processItemList([item], dir);
      });
    }

    // 1. Packliste atomar überschreiben: Vorhandene Daten (ohne Header) leeren
    const lastRowPackliste = sheetPackliste.getLastRow();
    if (lastRowPackliste > 1) {
      sheetPackliste.getRange(2, 1, lastRowPackliste - 1, HEADERS_PACKLISTE.length).clearContent();
    }

    // Neue Datenzeilen schreiben
    if (rowsToWrite.length > 0) {
      sheetPackliste.getRange(2, 1, rowsToWrite.length, HEADERS_PACKLISTE.length).setValues(rowsToWrite);
    }

    // 2. Neue Artikel an den Katalog anhängen
    if (newCatalogEntries.length > 0) {
      const lastRowKatalog = sheetKatalog.getLastRow();
      sheetKatalog.getRange(lastRowKatalog + 1, 1, newCatalogEntries.length, HEADERS_KATALOG.length).setValues(newCatalogEntries);
    }

    // Änderungen direkt flushen
    SpreadsheetApp.flush();

    // Strukturierte Liste für Client-Rückgabe aufbereiten
    const activeItemsResponse = rowsToWrite.map(r => ({
      id: r[0],
      direction: r[1],
      name: r[2],
      person: r[3],
      packed: r[4] === "packed",
      category: r[5],
      qty: r[6],
      lastModified: r[7]
    }));

    return createJsonResponse({
      status: "success",
      message: "Daten erfolgreich im Google Sheet aktualisiert",
      items_updated: rowsToWrite.length,
      new_catalog_items: newCatalogEntries.length,
      active_items: activeItemsResponse,
      timestamp: nowIso
    });

  } catch (error) {
    return createJsonResponse({
      status: "error",
      message: error.toString()
    });
  } finally {
    // Lock immer sicher freigeben
    lock.releaseLock();
  }
}

/**
 * ----------------------------------------------------------------------------
 * 3. HILFSFUNKTIONEN & INITIALISIERUNG
 * ----------------------------------------------------------------------------
 */

/**
 * Stellt sicher, dass die beiden Tabellenblätter mit korrekten Headern existieren.
 */
function ensureStructure(ss) {
  // 1. Blatt "Packliste"
  let sheetPackliste = ss.getSheetByName(SHEET_PACKLISTE);
  if (!sheetPackliste) {
    sheetPackliste = ss.insertSheet(SHEET_PACKLISTE);
    sheetPackliste.getRange(1, 1, 1, HEADERS_PACKLISTE.length).setValues([HEADERS_PACKLISTE]);
    sheetPackliste.getRange(1, 1, 1, HEADERS_PACKLISTE.length)
      .setFontWeight("bold")
      .setBackground("#0F766E")
      .setFontColor("#FFFFFF");
    sheetPackliste.setFrozenRows(1);
  }

  // 2. Blatt "Katalog"
  let sheetKatalog = ss.getSheetByName(SHEET_KATALOG);
  if (!sheetKatalog) {
    sheetKatalog = ss.insertSheet(SHEET_KATALOG);
    sheetKatalog.getRange(1, 1, 1, HEADERS_KATALOG.length).setValues([HEADERS_KATALOG]);
    sheetKatalog.getRange(1, 1, 1, HEADERS_KATALOG.length)
      .setFontWeight("bold")
      .setBackground("#0D9488")
      .setFontColor("#FFFFFF");
    sheetKatalog.setFrozenRows(1);

    // Initialen Katalog befüllen
    if (DEFAULT_CATALOG_ITEMS.length > 0) {
      sheetKatalog.getRange(2, 1, DEFAULT_CATALOG_ITEMS.length, HEADERS_KATALOG.length)
        .setValues(DEFAULT_CATALOG_ITEMS);
    }
  }
}

/**
 * Liest ein Sheet als Array von Objekten (Schlüssel = Header-Name).
 */
function readSheetAsObjects(sheet) {
  const lastRow = sheet.getLastRow();
  const lastCol = sheet.getLastColumn();
  if (lastRow <= 1 || lastCol < 1) return [];

  const values = sheet.getRange(1, 1, lastRow, lastCol).getValues();
  const headers = values[0];
  const rows = [];

  for (let i = 1; i < values.length; i++) {
    const rowObj = {};
    let hasData = false;
    for (let j = 0; j < headers.length; j++) {
      const header = headers[j];
      const val = values[i][j];
      rowObj[header] = val;
      if (val !== "" && val !== null && val !== undefined) hasData = true;
    }
    if (hasData) {
      rows.push(rowObj);
    }
  }
  return rows;
}

/**
 * Gibt ein Set mit allen Kleinschreib-Namen der Artikel im Katalog zurück.
 */
function getExistingCatalogItemNames(sheetKatalog) {
  const set = new Set();
  const lastRow = sheetKatalog.getLastRow();
  if (lastRow <= 1) return set;

  const names = sheetKatalog.getRange(2, 1, lastRow - 1, 1).getValues();
  names.forEach(r => {
    const name = String(r[0] || "").trim().toLowerCase();
    if (name) set.add(name);
  });
  return set;
}

/**
 * Heuristische Kategorie-Zuweisung für neue Artikel
 */
function deriveCategory(name) {
  const lower = name.toLowerCase();
  if (lower.includes("ski") || lower.includes("wandern") || lower.includes("bad") || lower.includes("sport") || lower.includes("bike")) {
    return "Sport & Outdoor";
  }
  if (lower.includes("tuch") || lower.includes("bett") || lower.includes("kissen") || lower.includes("laken")) {
    return "Textilien & Bettwäsche";
  }
  if (lower.includes("küche") || lower.includes("seife") || lower.includes("papier") || lower.includes("lumpen")) {
    return "Küche & Verbrauch";
  }
  if (lower.includes("hose") || lower.includes("pijama") || lower.includes("hemd") || lower.includes("shirt") || lower.includes("socken")) {
    return "Bekleidung & Wäsche";
  }
  return "Allgemein";
}

function capitalizeFirst(str) {
  if (!str) return "Haushalt";
  return str.charAt(0).toUpperCase() + str.slice(1);
}

/**
 * Erzeugt eine saubere JSON-Antwort mit MimeType JSON
 */
function createJsonResponse(data) {
  return ContentService.createTextOutput(JSON.stringify(data))
    .setMimeType(ContentService.MimeType.JSON);
}

/**
 * Manuelle Testfunktion: Kann im Apps Script Editor ausgeführt werden,
 * um die Tabellenblätter sofort zu initialisieren und zu testen.
 */
function testSetup() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  ensureStructure(ss);
  Logger.log("Tabellenstruktur 'Packliste' und 'Katalog' erfolgreich initialisiert!");
}
