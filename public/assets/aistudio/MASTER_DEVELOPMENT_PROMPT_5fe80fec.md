# PROJECT: WILD RIFT COMPANION
## FULL APPLICATION DEVELOPMENT FROM SCRATCH

### 1. YOUR ROLE

You are an expert team consisting of:

- Senior Software Architect
- Senior Android Application Developer
- UI/UX Designer
- Game Data Analyst
- League of Legends: Wild Rift Specialist
- AI Recommendation System Engineer
- Database Engineer
- Quality Assurance Engineer

Your mission is to independently design, develop, test, and deliver a complete Wild Rift companion application from scratch.

Do not simply create a UI mockup or a static prototype.

Build a functional application with real navigation, working features, structured game data, intelligent recommendation logic, and a maintainable architecture.

### 2. PROJECT VISION

Create an advanced League of Legends: Wild Rift companion application.

The application must help players:

1. Choose suitable champions.
2. Understand champion matchups.
3. Counter enemy champions.
4. Select optimal items based on enemy compositions.
5. Prepare team drafts.
6. Optimize runes and summoner spells.
7. Understand game strategies.
8. Improve individual performance.
9. Follow the latest available game meta and patches.

The application must function as a smart gaming assistant rather than merely a champion build database.

### 3. CORE FEATURES

#### MODULE A: CHAMPION DATABASE

Build a searchable champion database.

Each champion profile should include:

- Champion name
- Champion portrait
- Champion role
- Lane position
- Difficulty level
- Damage type
- Abilities
- Strengths
- Weaknesses
- Recommended builds
- Recommended runes
- Summoner spells
- Counter champions
- Matchup information

Include filtering by champion name, role, and lane.

#### MODULE B: SMART BUILD SYSTEM

Create a comprehensive champion build recommendation system.

Each build should support:

- Starting items
- Core items
- Situational items
- Defensive items
- Offensive items
- Boots and enchantments
- Runes
- Summoner spells
- Skill upgrade order

Build recommendations must consider:

- Champion identity
- Champion role
- Enemy team composition
- Enemy damage distribution
- Enemy crowd control
- Enemy healing
- Enemy defensive capabilities
- Current game patch

Do not recommend identical builds for every match.

#### MODULE C: INTELLIGENT COUNTER SYSTEM

Create a champion counter analysis engine.

The system must analyze:

- Champion versus champion matchups
- Lane advantages
- Damage compatibility
- Ability interactions
- Crowd control
- Mobility
- Sustain
- Early-game strength
- Mid-game strength
- Late-game strength

Present counter recommendations with explanations.

Do not rely exclusively on generic tier rankings.

#### MODULE D: SMART ITEM RECOMMENDATION

This is one of the most important features.

Develop an intelligent recommendation engine that evaluates enemy threats and recommends appropriate items.

Example rules:

IF enemy team has high AP burst:
Recommend appropriate Magic Resistance items.

IF enemy team has high Physical Damage:
Recommend Armor items.

IF enemy team has significant healing:
Recommend Grievous Wounds options.

IF enemy team contains high-health tanks:
Evaluate armor penetration, magic penetration, and percentage-health damage options.

IF enemy team has high attack speed:
Evaluate appropriate defensive options.

Every recommendation must contain:

1. Recommended item.
2. Reason for recommendation.
3. Enemy champion or threat being countered.
4. Expected tactical benefit.
5. Recommended purchase priority.

The engine must support multiple simultaneous threats.

Do not force a single recommendation when several defensive or offensive adjustments are necessary.

#### MODULE E: DRAFT ASSISTANT

Create a team composition analysis system.

Allow users to select:

- Allied champions
- Enemy champions
- Intended lane
- Preferred role

Analyze:

- Team synergy
- Damage balance
- Frontline availability
- Crowd control
- Engage potential
- Disengage potential
- Team fight capabilities
- Split-push potential
- Enemy counters

Generate practical draft recommendations.

#### MODULE F: AI COACH

Create a coaching module that provides:

- Champion-specific gameplay advice
- Lane management
- Wave management
- Trading strategies
- Objective control
- Map awareness
- Rotation strategies
- Team fight positioning
- Early, mid, and late-game strategies

Coaching advice must be specific to the selected champion and role.

#### MODULE G: META AND PATCH DATABASE

Create a patch-aware data architecture.

Each champion, item, rune, build, and recommendation should support version information.

Include:

- Current known patch
- Champion balance changes
- Item changes
- Rune changes
- Tier lists
- Role rankings
- Build updates

Never silently present outdated data as current information.

Display the data version and last updated date.

### 4. REFERENCE DATA SOURCES

Use these websites as research and reference sources:

1. https://lolwildriftbuild.com/category/tips-tricks
2. https://wildriftcore.com/en/guides
3. https://wildriftcore.com/en/counter-picks
4. https://wildriftcore.com/en/draft/
5. https://wildriftcore.com/en/coach
6. https://www.wildriftmeta.com/
7. https://www.wildriftguides.com/

Use multiple sources when possible.

Do not assume that every website provides an API.

Do not bypass website restrictions or scrape data in violation of their terms.

If automatic data retrieval is unavailable, implement a structured data import system and make the data source transparent.

Distinguish verified data from estimated or manually entered recommendations.

### 5. UI/UX DESIGN

Design a modern gaming application.

Visual direction:

- Dark gaming interface
- Premium appearance
- Clean typography
- High-quality champion artwork
- Clear item icons
- Modern cards
- Smooth transitions
- Responsive layouts
- Intuitive navigation

Suggested main navigation:

1. Home
2. Champions
3. Builds
4. Counter
5. Draft
6. AI Coach
7. Settings

The application should support Thai and English.

Thai should be the default language.

### 6. TECHNICAL REQUIREMENTS

Build a maintainable and scalable application.

If the environment supports native Android development:

- Kotlin
- Jetpack Compose
- Material Design 3
- MVVM architecture
- Repository pattern
- Room database
- Retrofit where appropriate
- Coroutines and Flow

If the environment only supports web application development:

- React
- TypeScript
- Responsive design
- PWA support
- Structured service layer
- Persistent local storage
- Android-compatible architecture for future packaging

Do not pretend that a web application is a native Android APK.

Clearly document the Android packaging process if native Android output is unavailable.

Separate:

- UI components
- Business logic
- Recommendation engine
- Data repositories
- Game data
- Configuration
- External integrations

Avoid hardcoding game data directly into UI components.

### 7. DATA ARCHITECTURE

Create structured models for:

- Champion
- Ability
- Item
- Rune
- Spell
- Build
- Matchup
- Counter
- Draft
- Patch
- Recommendation
- User preferences

Every game data record should support source attribution and patch version.

Create a data validation mechanism to prevent invalid champion, item, and rune references.

### 8. IMPORTANT DEVELOPMENT RULES

- Build functional features, not decorative mockups.
- Do not leave navigation buttons non-functional.
- Do not use fabricated statistics as verified game data.
- Do not invent unavailable APIs.
- Do not silently ignore errors.
- Do not delete working functionality when adding new features.
- Keep the project modular.
- Maintain readable and documented code.
- Implement appropriate loading, empty, and error states.
- Support offline access to previously downloaded game data where practical.

### 9. DEVELOPMENT WORKFLOW

Work through the following phases:

PHASE 1 — Architecture and planning
- Define application architecture.
- Define data models.
- Define navigation.
- Define module dependencies.

PHASE 2 — Application foundation
- Initialize the project.
- Implement the design system.
- Implement navigation.
- Implement local data storage.

PHASE 3 — Core gaming features
- Champion database.
- Build system.
- Counter system.
- Item recommendation engine.
- Draft assistant.

PHASE 4 — Advanced features
- AI Coach.
- Macro Guide.
- Meta and Patch system.
- Favorites.

PHASE 5 — Quality assurance
- Test all navigation.
- Test all features.
- Validate data relationships.
- Fix runtime errors.
- Fix build errors.
- Check responsive layouts.

PHASE 6 — DELIVERY
- Provide complete source code.
- Provide project structure.
- Provide setup instructions.
- Provide build instructions.
- Provide Android APK packaging instructions when applicable.

### 10. FINAL OBJECTIVE

Deliver a functional, scalable, and visually polished Wild Rift companion application.

The most important competitive advantage must be the combination of:

CHAMPION ANALYSIS
+
COUNTER ANALYSIS
+
SMART ITEM RECOMMENDATION
+
DRAFT ANALYSIS
+
GAMEPLAY COACHING

The application should help players understand not only WHAT to build, but WHY they should build it and WHEN they should adapt their strategy.

Start by analyzing the development environment and defining the implementation architecture.

Then proceed with actual implementation.

Do not stop after generating a project plan or UI mockup.