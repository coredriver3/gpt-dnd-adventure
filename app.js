const state = {
  language: "de",
  images: true,
  dead: false,
  turn: 0,
  health: 10,
  maxHealth: 10,
  resolve: 6,
  maxResolve: 6,
  resources: {
    coin: 12,
    rations: 3,
    torch: 2,
  },
  inventory: ["weathered map", "knife", "ink kit"],
  effects: [],
  nodes: [],
  character: null,
};

const encounters = {
  "dark-fantasy": [
    "a drowned chapel whose bell rings under the marsh water",
    "a toll bridge held by deserters with clean boots and dirty blades",
    "a lantern-lit crypt where the walls remember names",
    "a ruined watchtower leaning over a road of black glass",
  ],
  "arcane-city": [
    "a masked auction beneath the old observatory",
    "a canal district where reflections answer before people speak",
    "a locked archive guarded by contract-bound statues",
    "a rooftop chase across copper rain collectors",
  ],
  "post-empire": [
    "a collapsed imperial relay station humming with leftover commands",
    "a salt flat crossed by broken war machines",
    "a village built inside the ribs of a dead skyship",
    "a sealed bunker with fresh footprints in the dust",
  ],
  "german-folklore": [
    "a fir forest where every path returns to the same shrine",
    "a millpond that shows tomorrow's injuries",
    "a charcoal burner who trades in secrets instead of coin",
    "a hilltop ruin where the moonlight has weight",
  ],
};

const settingLabels = {
  "dark-fantasy": "Dark fantasy borderlands",
  "arcane-city": "Arcane city intrigue",
  "post-empire": "Post empire ruins",
  "german-folklore": "German folklore woods",
};

const outcomes = [
  { min: 1, max: 4, grade: "critical failure", health: -4, resolve: -1 },
  { min: 5, max: 8, grade: "failure", health: -2, resolve: 0 },
  { min: 9, max: 12, grade: "mixed result", health: -1, resolve: -1 },
  { min: 13, max: 16, grade: "success with cost", health: 0, resolve: -1 },
  { min: 17, max: 19, grade: "clean success", health: 0, resolve: 0 },
  { min: 20, max: 20, grade: "critical success", health: 0, resolve: 1 },
];

const $ = (id) => document.getElementById(id);

function rand(list) {
  return list[Math.floor(Math.random() * list.length)];
}

function roll(sides = 20, mod = 0) {
  return Math.floor(Math.random() * sides) + 1 + mod;
}

function validateInput(text) {
  const trimmed = text.trim();
  if (trimmed.length < 2) return false;
  if (/https?:|<script|drop table|ignore previous/i.test(trimmed)) return false;
  const words = trimmed.split(/\s+/).filter(Boolean);
  if (!trimmed.startsWith("[") && words.length < 2) return false;
  return /^[\p{L}\p{N}\s.,!?;:'"()[\]/+\-äöüÄÖÜß]+$/u.test(trimmed);
}

function needsRoll(text) {
  return !/^\[/.test(text) && !/(look|listen|warte|schaue|untersuche vorsichtig|inspect|observe)$/i.test(text.trim());
}

function outcomeFor(total, danger) {
  const adjusted = Math.max(1, Math.min(20, total - danger));
  return outcomes.find((item) => adjusted >= item.min && adjusted <= item.max) || outcomes[2];
}

function renderStats() {
  $("characterSummary").textContent = state.character
    ? `${state.character.name} - ${state.character.concept}`
    : "No character";
  $("settingSummary").textContent = state.character
    ? settingLabels[state.character.setting]
    : "Create a new run first";
  $("healthText").textContent = `${state.health} / ${state.maxHealth}`;
  $("resolveText").textContent = `${state.resolve} / ${state.maxResolve}`;
  $("healthBar").style.setProperty("--value", `${Math.max(0, (state.health / state.maxHealth) * 100)}%`);
  $("resolveBar").style.setProperty("--value", `${Math.max(0, (state.resolve / state.maxResolve) * 100)}%`);

  $("resources").innerHTML = Object.entries(state.resources)
    .map(([key, value]) => `<li><span>${key}</span><strong>${value}</strong></li>`)
    .join("");
  $("inventory").innerHTML = state.inventory.map((item) => `<li>${item}</li>`).join("");
  document.body.classList.toggle("dead", state.dead);
}

function addNode({ type, text, rollText = "no roll", imagePrompt, consequences = [] }) {
  const shouldScroll = state.nodes.length > 0;
  const node = $("nodeTemplate").content.firstElementChild.cloneNode(true);
  node.querySelector(".node-type").textContent = type;
  node.querySelector(".roll-chip").textContent = rollText;
  node.querySelector(".node-text").textContent = text;
  const image = node.querySelector(".node-image");
  image.hidden = !state.images || !imagePrompt;
  image.dataset.prompt = imagePrompt || "";
  const consequenceBox = node.querySelector(".consequences");
  consequenceBox.innerHTML = consequences
    .map((item) => `<span class="${item.good ? "good" : item.bad ? "bad" : ""}">${item.text}</span>`)
    .join("");
  $("timeline").appendChild(node);
  if (shouldScroll) node.scrollIntoView({ behavior: "smooth", block: "end" });
  state.nodes.push({ type, text, rollText, imagePrompt, consequences });
  renderStats();
}

function intro() {
  const { name, concept, setting } = state.character;
  const place = rand(encounters[setting]);
  addNode({
    type: "scene",
    text:
      state.language === "de"
        ? `${name}, ${concept}, erreicht ${place}. Die Luft schmeckt nach Regen und Metall. Deine Vorräte sind knapp, und jede falsche Entscheidung kann bleibende Folgen haben.`
        : `${name}, ${concept}, reaches ${place}. The air tastes of rain and metal. Supplies are thin, and bad decisions can leave permanent scars.`,
    imagePrompt: `${setting}, ${place}, cinematic adventure concept art`,
    consequences: [
      { text: "inventory tracked" },
      { text: "death enabled", bad: true },
      { text: "contextual dice difficulty" },
    ],
  });
}

function commandMode(text) {
  const command = text.slice(1, -1).trim().toLowerCase();
  if (command.startsWith("roll")) {
    const match = command.match(/d(\d+)([+-]\d+)?/);
    const sides = match ? Number(match[1]) : 20;
    const mod = match?.[2] ? Number(match[2]) : 0;
    const total = roll(sides, mod);
    addNode({
      type: "command",
      text: `Manual roll: d${sides}${mod ? (mod > 0 ? `+${mod}` : mod) : ""} = ${total}. The story does not advance.`,
      rollText: `${total}`,
      consequences: [{ text: "command mode" }],
    });
    return;
  }

  const reports = {
    inventory: `Inventory: ${state.inventory.join(", ")}.`,
    status: `Health ${state.health}/${state.maxHealth}, Resolve ${state.resolve}/${state.maxResolve}. Effects: ${state.effects.join(", ") || "none"}.`,
    help: "Commands: [inventory], [status], [roll d20+2], [image on], [image off].",
    "image on": "Scene image cards enabled.",
    "image off": "Scene image cards disabled.",
  };

  if (command === "image on") state.images = true;
  if (command === "image off") state.images = false;

  addNode({
    type: "command",
    text: reports[command] || "Unknown command. Try [help]. The story does not advance.",
    consequences: [{ text: "no story advance" }],
  });
}

function narrateAction(text) {
  const { setting } = state.character;
  const place = rand(encounters[setting]);
  const danger = Math.min(4, Math.floor(state.turn / 3) + (state.health <= 4 ? 1 : 0));
  const natural = needsRoll(text) ? roll(20) : null;
  const result = natural ? outcomeFor(natural, danger) : { grade: "information gained", health: 0, resolve: 0 };
  const consequences = [];

  state.turn += 1;
  state.health = Math.max(0, Math.min(state.maxHealth, state.health + result.health));
  state.resolve = Math.max(0, Math.min(state.maxResolve, state.resolve + result.resolve));

  if (natural && natural >= 17 && Math.random() > 0.42) {
    const reward = rand(["silver token", "clean bandage", "sealed clue", "warm meal"]);
    state.inventory.push(reward);
    consequences.push({ text: `gained ${reward}`, good: true });
  }

  if (natural && natural <= 8 && Math.random() > 0.35) {
    const spent = Object.keys(state.resources).find((key) => state.resources[key] > 0);
    if (spent) {
      state.resources[spent] -= 1;
      consequences.push({ text: `lost 1 ${spent}`, bad: true });
    }
  }

  if (state.health === 0) {
    state.dead = true;
    consequences.push({ text: "character died", bad: true });
  }

  if (result.health < 0) consequences.push({ text: `${result.health} health`, bad: true });
  if (result.resolve < 0) consequences.push({ text: `${result.resolve} resolve`, bad: true });
  if (result.resolve > 0) consequences.push({ text: `+${result.resolve} resolve`, good: true });
  consequences.push({ text: result.grade, good: natural >= 17, bad: natural <= 8 });

  const rollText = natural ? `d20 ${natural} / ${result.grade}` : "no roll";
  const deathLine = state.dead
    ? state.language === "de"
      ? " Das ist kein Rückschlag mehr: Deine Geschichte endet hier."
      : " This is no longer a setback: your story ends here."
    : "";

  const textDe = `Du willst: "${text}". In ${place} kippt die Lage in Richtung ${result.grade}. Gute Würfe öffnen nicht automatisch Schatzkammern; schlechte Würfe schaffen Probleme, die zur Szene passen. ${describe(result.grade, "de")}${deathLine}`;
  const textEn = `You attempt: "${text}". At ${place}, the situation turns into a ${result.grade}. Good rolls do not automatically create treasure; bad rolls create problems that fit the scene. ${describe(result.grade, "en")}${deathLine}`;

  addNode({
    type: "story point",
    text: state.language === "de" ? textDe : textEn,
    rollText,
    imagePrompt: `${setting}, ${place}, player action: ${text}, ${result.grade}, cinematic fantasy`,
    consequences,
  });
}

function describe(grade, language) {
  const de = {
    "critical failure": "Du bekommst, was du wolltest, nur wenn du dafür blutest; sonst nimmt dir die Welt etwas Wichtiges.",
    failure: "Der Versuch scheitert und zwingt dich in eine schlechtere Position.",
    "mixed result": "Du machst Fortschritt, aber bezahlst sofort dafür.",
    "success with cost": "Du erreichst dein Ziel, doch die Situation bleibt gefährlich.",
    "clean success": "Du setzt dich klar durch und gewinnst Kontrolle über den Moment.",
    "critical success": "Du dominierst die Szene, aber die Belohnung bleibt plausibel.",
    "information gained": "Du sammelst Details, ohne die Lage direkt zu eskalieren.",
  };
  const en = {
    "critical failure": "You get what you wanted only if you bleed for it; otherwise the world takes something important.",
    failure: "The attempt fails and forces you into a worse position.",
    "mixed result": "You make progress, but pay for it immediately.",
    "success with cost": "You reach your goal, yet the situation stays dangerous.",
    "clean success": "You clearly prevail and take control of the moment.",
    "critical success": "You dominate the scene, but the reward stays plausible.",
    "information gained": "You gather detail without escalating the situation directly.",
  };
  return language === "de" ? de[grade] : en[grade];
}

function createGame() {
  const name = $("characterName").value.trim() || "Mara";
  const concept = $("characterConcept").value.trim() || "traveller";
  const setting = $("setting").value;

  Object.assign(state, {
    dead: false,
    turn: 0,
    health: 10,
    resolve: 6,
    resources: { coin: 12, rations: 3, torch: 2 },
    inventory: ["weathered map", "knife", "ink kit"],
    effects: [],
    nodes: [],
    character: { name, concept, setting },
  });
  $("timeline").innerHTML = "";
  $("setupForm").hidden = true;
  $("actionForm").hidden = false;
  intro();
}

$("setupForm").addEventListener("submit", (event) => {
  event.preventDefault();
  createGame();
});

function showSetup() {
  Object.assign(state, {
    dead: false,
    turn: 0,
    health: 10,
    resolve: 6,
    resources: { coin: 12, rations: 3, torch: 2 },
    inventory: ["weathered map", "knife", "ink kit"],
    effects: [],
    nodes: [],
    character: null,
  });
  $("timeline").innerHTML = "";
  $("setupForm").hidden = false;
  $("actionForm").hidden = true;
  renderStats();
  $("characterName").focus();
}

$("actionForm").addEventListener("submit", (event) => {
  event.preventDefault();
  const input = $("playerInput");
  const value = input.value.trim();
  input.classList.remove("invalid");

  if (!state.character) {
    showSetup();
    return;
  }

  if (state.dead) {
    addNode({ type: "system", text: "This character is dead. Start a new game.", consequences: [{ text: "fatal state", bad: true }] });
    return;
  }

  if (!validateInput(value)) {
    input.classList.add("invalid");
    addNode({
      type: "rejected input",
      text: "Input rejected: use coherent German or English actions that fit the current scene. Commands belong in square brackets.",
      consequences: [{ text: "story did not advance" }],
    });
    return;
  }

  if (/^\[.*\]$/.test(value)) commandMode(value);
  else narrateAction(value);
  input.value = "";
});

$("newGameBtn").addEventListener("click", showSetup);
$("imageToggle").addEventListener("click", () => {
  state.images = !state.images;
  $("imageToggle").textContent = state.images ? "Images on" : "Images off";
  document.querySelectorAll(".node-image").forEach((image) => {
    image.hidden = !state.images;
  });
});
$("languageToggle").addEventListener("click", () => {
  state.language = state.language === "de" ? "en" : "de";
  $("languageToggle").textContent = state.language.toUpperCase();
  $("playerInput").placeholder =
    state.language === "de"
      ? "Was tust du? Oder [inventory], [status], [roll d20+2] ..."
      : "What do you do? Or [inventory], [status], [roll d20+2] ...";
});

showSetup();
