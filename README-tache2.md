# IFT3913 — Tâche 2 : `MediaType` et `Patterns` (Apache Tika)

**Équipe :** Toky Erick Rabenantoandro , Luc Brennan

[![tache2](https://github.com/LucBrennan/tika/actions/workflows/tache2.yml/badge.svg)](https://github.com/LucBrennan/tika/actions/workflows/tache2.yml)

| Étape de l'énoncé | Section |
|---|---|
| Classes choisies et justification | [1](#1-classes-choisies) |
| ChatUniTest et modèle local | [2](#2-chatunitest-dans-le-pipeline-maven) |
| Tests générés : où, compilation, corrections, critique | [3](#3-tests-générés) |
| Oracles de l'IA vs écrits à la main | [4](#4-oracles) |
| pitest : scores avant/après, mutants détectés | [5](#5-analyse-de-mutation) |
| Tests ajoutés à la main | [6](#6-tests-écrits-à-la-main) |
| GitHub Action | [7](#7-github-action) |
| Déclaration d'utilisation de l'IA | [8](#8-déclaration-dutilisation-de-lia) |

**Fichiers :** configuration dans [`tika-core/pom.xml`](tika-core/pom.xml) et [`Modelfile`](Modelfile) ; tests dans [`tika-core/src/test/java/org/apache/tika/mime/`](tika-core/src/test/java/org/apache/tika/mime/) ; sortie brute de ChatUniTest dans [`tache2/chatunitest-raw/`](tache2/chatunitest-raw/) ; rapports : [JaCoCo](tache2/jacoco-baseline/index.html), pitest [tests d'origine](tache2/pitest-baseline-isolated/index.html), [+ tests générés](tache2/pitest-after-generated/index.html), [final](tache2/pitest-final/index.html).

## 1. Classes choisies

`org.apache.tika.mime.MediaType` (analyse et représentation des types MIME) et `org.apache.tika.mime.Patterns` (association nom de fichier → type MIME), dans le module `tika-core`. Tests existants : `MediaTypeTest`, `PatternsTest`.

**Couverture JaCoCo** (toute la suite de tika-core) :

| Classe | Lignes | Branches |
|---|---|---|
| `MediaType` | 125/155 (80,6 %) | 67/90 (74,4 %) |
| `Patterns` | 69/77 (89,6 %) | 42/64 (65,6 %) |

| Méthodes jamais exécutées | Méthodes partiellement exécutées (instructions / branches) |
|---|---|
| `MediaType.set(String...)`, `set(MediaType...)`, `audio`, `video`, `MediaType(MediaType, String, String)`, `MediaType(MediaType, Charset)` | `MediaType.parse` 76 % / 72 % (lignes 277–278 jamais exécutées), `union` 22 % / 25 %, `equals` 85 % / 50 % ; `Patterns.addName` 38 % / 12 %, `addGlob` 38 % / 12 %, `matches` 92 % / 91 %, `compile` 91 % / 87 % |

**pitest avec les tests d'origine*** :

| Classe | Tués | Survivants | Non couverts | Score |
|---|---|---|---|---|
| `MediaType` | 35 | 32 | 18 | 41,2 % |
| `Patterns` | 12 | 17 | 15 | 27,3 % |

\* `MediaTypeTest`, `PatternsTest`, `CharsetUtilsTest` et `LookaheadInputStreamTest`, exécutés ensemble.

Mutants non tués, par méthode :

| Classe | Méthode | Survivants | Non couverts |
|---|---|---|---|
| `MediaType` | `application`, `audio`, `image`, `text`, `video` | 0 | 1 chacune |
| `MediaType` | `set` (×2) | 0 | 4 |
| `MediaType` | `parse` | 5 | 3 |
| `MediaType` | `isSimpleName` | 12 | 0 |
| `MediaType` | `union` | 2 | 5 |
| `MediaType` | `equals` | 3 | 1 |
| `MediaType` | `getBaseType`, `getSubtype`, `hasParameters` | 2 chacune | 0 |
| `MediaType` | `getType`, `compareTo` | 1 chacune | 0 |
| `MediaType` | constructeur | 2 | 0 |
| `Patterns` | `matches` | 0 | 9 |
| `Patterns` | `add`, `compile` | 6 chacune | 0 |
| `Patterns` | `addExtension` | 3 | 0 |
| `Patterns` | `addName`, `addGlob` | 1 chacune | 3 chacune |

**Justification des classes et des méthodes**

Nous avons retenu `MediaType` et `Patterns` parce que leurs méthodes transforment des chaînes en types, sans fichier ni réseau, et qu'elles ont déjà leurs tests (`MediaTypeTest`, `PatternsTest`). Ces tests laissent pourtant du code sans vérification. Six méthodes de `MediaType` ne sont jamais exécutées par la suite de tika-core (les deux `set`, `audio`, `video` et les constructeurs `MediaType(MediaType, String, String)` et `MediaType(MediaType, Charset)`), et seules 74,4 % et 65,6 % des branches sont couvertes.

Exécuter le code ne suffit pas à le vérifier, puisque pitest ne tue que 41,2 % des mutants de `MediaType` et 27,3 % de ceux de `Patterns`. Il reste 32 et 17 mutants survivants (le code est exécuté, mais aucune assertion ne remarque le changement) et 18 et 15 jamais atteints. Par exemple, `PatternsTest` charge toute la base MIME de Tika, soit des milliers d'appels à `Patterns.add`, sans vérifier ce travail, et n'appelle jamais `matches` avec un vrai nom de fichier.

Nous visons les méthodes qui concentrent ces mutants (tableau ci-dessus). Pour `MediaType`, ce sont `isSimpleName` (12 survivants), `parse`, `union`, `equals`, `set` et les fabriques, et pour `Patterns`, `add`, `addName`, `addGlob`, `addExtension`, `matches` et `compile`.

`CharsetUtils` (0 survivant, 5 non couverts) et `LookaheadInputStream` ont aussi été mesurées, puis écartées.

## 2. ChatUniTest dans le pipeline Maven

ChatUniTest 2.1.1 est déclaré comme plugin dans [`tika-core/pom.xml`](tika-core/pom.xml), sans être lié à une phase : la CI ne l'exécute donc pas.

```xml
<plugin>
  <groupId>io.github.zju-aces-ise</groupId>
  <artifactId>chatunitest-maven-plugin</artifactId>
  <version>2.1.1</version>
  <configuration>
    <apiKeys>ollama</apiKeys>
    <model>codeqwen:v1.5-chat</model>
    <url>http://localhost:11434/v1/chat/completions</url>
    <thread>false</thread>
    <testNumber>1</testNumber>
    <maxRounds>3</maxRounds>
    <merge>false</merge>
  </configuration>
</plugin>
```

- **Modèle :** `qwen2.5-coder:3b` dans Ollama 0.35.1, sur processeur (Windows 11, 16 Go de RAM, carte graphique intégrée). Le [`Modelfile`](Modelfile) (`FROM qwen2.5-coder:3b`, `PARAMETER num_ctx 16384`) crée le nom `codeqwen:v1.5-chat`, l'un des seuls que ChatUniTest 2.1.1 accepte, avec un contexte de 16 384 jetons pour ses invites (jusqu'à environ 10 900 jetons).
- **Taille du modèle :** ChatUniTest abandonne toute réponse après 5 minutes.

| Modèle | Écriture | Lecture | Requête réaliste |
|---|---|---|---|
| `qwen2.5-coder:7b` | 3,67 jetons/s | 11,55 jetons/s | plus de 5 min estimées (réponse de 1 024 jetons : environ 4 min 40 s) |
| `qwen2.5-coder:3b` | 7,17 jetons/s | 29,13 jetons/s | 3 min 14 s (classe `Patterns` entière) |

- **Options :** une requête à la fois (`thread=false`), 1 test par méthode (défaut : 5), 3 tours (défaut : 5), pas de classe « suite » JUnit 4 (`merge=false`).
- **Dépendances de test ajoutées :** `mockito-core`, `mockito-junit-jupiter`, `junit-jupiter-params`, car l'invite de ChatUniTest demande « JUnit5 and Mockito ».
- **Commandes** (depuis `tika-core/`) : `../mvnw chatunitest:class -DselectClass=Patterns`, puis `-DselectClass=MediaType`.
- **Preuve d'installation :** [`tache2/chatunitest-plugin-check.txt`](tache2/chatunitest-plugin-check.txt).

## 3. Tests générés

- **Où :** sortie brute dans [`tache2/chatunitest-raw/`](tache2/chatunitest-raw/) (tests produits, erreurs de compilation de chaque tour, journaux) ; versions corrigées dans `tika-core/src/test/java/org/apache/tika/mime/` (`MediaType_*_Test.java`, `Patterns_*_Test.java`, chaque correctif marqué `// FIX (...)`).
- **Méthodes ignorées par ChatUniTest :** constructeurs, méthodes privées, et noms commençant par `get`, `set` ou `is` (donc `MediaType.set(...)`, `getBaseType`, `getType`, `getSubtype`).
- **Durées :** 27 min 49 s (`Patterns`, 9 requêtes), 59 min 34 s (`MediaType`).

| Méthode | Message de ChatUniTest | Tests qui passent tels quels | Après correction |
|---|---|---|---|
| `Patterns.add` (×2), `matches` | échec de compilation aux 3 tours | aucun fichier | 22 tests repris des dernières tentatives |
| `MediaType.application` | « compile and execute successfully » | 3/8 | 7/7 |
| `MediaType.image` | « compile and execute successfully » | 0/4 | 2/2 |
| `MediaType.text` | « compile and execute successfully » | 3/16 | 4/4 |
| `MediaType.video`, `hasParameters` | réussi (tests en échec retirés par ChatUniTest) | 11/11 | 11/11 |
| `MediaType.equals` | « compile and execute successfully » | 15/17 | 17/17 |
| `MediaType.compareTo` | « compile and execute successfully » | 0/5 | 5/5 |
| `MediaType.audio`, `parse`, `toString`, `hashCode` | échec de compilation aux 3 tours | aucun fichier | non repris |
| **Total `MediaType`** | | **32/61** | **46/46** |

**Erreurs de compilation du dernier tour** (fichiers `*_CompilationError_2.txt`) :

| Méthode | Erreurs |
|---|---|
| `Patterns.add`, `matches` | paquets `org.junit.jupiter.params` et `org.mockito.junit.jupiter` absents ; accès aux champs privés `names` et `extensions` ; `String` passée là où un `MediaType` est attendu |
| `MediaType.parse` | imports statiques de classes ; constantes privées de `MediaType` recopiées |
| `MediaType.audio` | `MediaType.audio` utilisé comme un champ |
| `MediaType.hashCode` | annotations mal importées ; `@CsvSource` converti en `MediaType` |
| `MediaType.toString` | constructeur inexistant `MediaType(String, String, Charset)` |

**Compilent-ils et s'exécutent-ils sans intervention ?** Non : aucun des 10 fichiers. Tous ont demandé des corrections de forme exigées par Checkstyle (en-tête de licence, imports `*`, imports inutiles). Corrections de fond :

| Fichier | Corrections |
|---|---|
| `MediaType_application` | 4 valeurs attendues corrigées, 3 assertions et 1 test retirés |
| `MediaType_text` | 1 valeur attendue corrigée, 12 tests retirés |
| `MediaType_equals` | 6 assertions retirées (2 tests) |
| `MediaType_image` | préparation Mockito (`@InjectMocks`) retirée, 4 appels par réflexion remplacés par des appels directs, 1 valeur attendue corrigée, 2 tests retirés |
| `MediaType_compareTo` | préparation Mockito (`@InjectMocks`) retirée |
| `MediaType_video`, `MediaType_hasParameters` | aucune |
| `Patterns_*` (3 fichiers) | 27 tests générés, 22 conservés, 31 correctifs : 10 `oracle`, 11 `access`, 4 `setup`, 4 `compile`, 2 retraits (5 tests) ([`tache2/fixes-log.md`](tache2/fixes-log.md)) |

**Explication et critique des tests générés**

Aucun des 10 fichiers n'était utilisable tel quel. Tous ont demandé des corrections de forme (Checkstyle), et 8 des corrections de fond. Pour `Patterns`, aucun fichier n'a compilé, car les mêmes erreurs (accès aux champs privés `names` et `extensions`, `String` passée à `new MimeType(...)`) reviennent aux trois tours de réparation, comme pour quatre méthodes de `MediaType`. Le modèle ne sait donc pas corriger ses propres erreurs de compilation. Pour `MediaType`, 32 des 61 tests réussissaient tels que générés, et les 46 conservés réussissent tous après correction.

Le modèle attend souvent un comportement que Tika n'a pas. Les 12 tests retirés de `text` attendaient `null` pour des entrées comme `"invalid/type"`, alors que `parse` ne vérifie que la syntaxe. Dans `application`, il attend `"application/json"` pour `parse("application/json; charset=utf-8").toString()`, alors que Tika garde le paramètre, et `null` pour `parse("application/json;")`, que Tika accepte. ChatUniTest a pourtant affiché « compile and execute successfully » pour `image` et `compareTo`, dont aucun test ne réussissait (0 sur 4 et 0 sur 5). L'échec venait de l'initialisation Mockito (`@InjectMocks MediaType`), que son filtre d'erreurs ne reconnaît pas.

Beaucoup de tests n'apportent rien. Par exemple, 13 des 17 tests de `equals` ont un corps identique, et les 10 tests de `hasParameters` ne testent jamais le cas `false`. Pour `Patterns`, il a fallu 31 correctifs (10 `oracle`, 11 `access`, 4 `setup`, 4 `compile` et 2 retraits portant sur 5 tests). Les tests lisaient des champs privés, simulaient des méthodes que `Patterns` n'appelle jamais (`getName()`, `getExtension()`) et portaient des noms trompeurs (`testAddExtension` enregistre un nom exact, `testAddGlobConflict` une extension).

Une fois corrigés, ces tests sont utiles. Le score passe de 41,2 % à 63,5 % pour `MediaType` et de 27,3 % à 88,6 % pour `Patterns`, pour environ 1 h 27 de génération. Les valeurs corrigées suivent le comportement réel de Tika et ses propres tests (par exemple `testParseNoParamsWithSemi`), plusieurs tests faux ont été retirés plutôt que corrigés, et des oracles simples comme `testParseSimple` tuent même des mutants dans des méthodes que ChatUniTest saute. Les mutants restants de `MediaType` viennent des méthodes que l'outil saute (`set`, qu'il prend pour un mutateur, les constructeurs et `getBaseType`), de la génération ratée d'`audio` et d'entrées jamais essayées (charset en tête, cas `false` de `hasParameters`, `equals` avec un autre type d'objet). Ceux de `Patterns` viennent de deux cas que le modèle n'a pas imaginés, soit un conflit sur un vrai glob et un nom exact face à une extension.

## 4. Oracles

Les oracles de Tika pour `MediaType` sont précis, avec des chaînes exactes même pour les cas tordus. Par exemple, `testOddParameters` exige que `parse("text/html;; charset=UTF-8")` s'écrive `"text/html; charset=UTF-8"`, et `testParseNoParamsWithSemi` que `parse("text/html;")` soit accepté sans paramètre. Le test `testParseWithParams` est plus faible, puisqu'il se contente de `assertNotNull`, d'une taille et de booléens. Ceux de `PatternsTest` sont pauvres, car `testAdd` et `testMatches` vérifient seulement qu'un `null` lève `IllegalArgumentException`, `testExtension` lit `getExtension()` après `add("*.doc")` sans appeler `matches()`, et rien ne teste la priorité entre motifs.

Les oracles générés ont aussi des valeurs exactes, mais souvent fausses. Par exemple, `assertNull(MediaType.parse("application/json;"))` est le contraire de ce que vérifie `testParseNoParamsWithSemi`, qui accepte `parse("text/html;")`, et `assertEquals("application/json", MediaType.parse("application/json; charset=utf-8").toString())` oublie que `toString()` garde les paramètres, comme dans `testOddParameters`. Ils sont aussi souvent faibles ou redondants, avec seulement des `assertTrue` pour `hasParameters`, un type ou un mock unique pour tous les tests de `Patterns` (impossible alors de savoir quel motif a répondu) et 13 tests identiques pour `equals`. Quand ils sont justes, ils restent simples mais utiles (`testParseSimple`). Tika est donc exigeant sur `MediaType` et peu sur `Patterns`, alors que le modèle écrit des valeurs exactes, souvent sans connaître le comportement réel.

## 5. Analyse de mutation

pitest 1.25.9 (`pitest-junit5-plugin` 1.2.3, mutateurs par défaut) dans [`tika-core/pom.xml`](tika-core/pom.xml), avec `mutationUnitSize=1` (un mutant par JVM). Depuis `tika-core/` :

```
../mvnw test-compile org.pitest:pitest-maven:mutationCoverage '-Dpit.targetTests=${pit.baselineTests}' -DreportsDirectory=../tache2/pitest-baseline-isolated
../mvnw test-compile org.pitest:pitest-maven:mutationCoverage '-Dpit.targetTests=${pit.baselineTests},${pit.generatedTests}' -DreportsDirectory=../tache2/pitest-after-generated
../mvnw test-compile org.pitest:pitest-maven:mutationCoverage -DreportsDirectory=../tache2/pitest-final
```

| Classe | Mesure | Tués | Survivants | Non couverts | Score de mutation |
|---|---|---|---|---|---|
| `MediaType` | Tests d'origine | 35 | 32 | 18 | 41,2 % |
| `MediaType` | + tests générés | 54 | 19 | 12 | 63,5 % |
| `MediaType` | + tests écrits à la main | 76 | 9 | 0 | 89,4 % |
| `Patterns` | Tests d'origine | 12 | 17 | 15 | 27,3 % |
| `Patterns` | + tests générés | 39 | 2 | 3 | 88,6 % |
| `Patterns` | + tests écrits à la main | 44 | 0 | 0 | 100 % |

**Les tests générés détectent-ils tous les mutants ?** Non : après eux, 31 mutants de `MediaType` et 5 de `Patterns` restent vivants ([rapport](tache2/pitest-after-generated/index.html)).

**JVM partagée ou un mutant par JVM**

Par défaut, pitest exécute plusieurs mutants dans la même JVM, ce qui fausse les résultats de `MediaType`. On obtient alors 48, 65 puis 84 mutants tués sur 85, contre 35, 54 puis 76 avec un mutant par JVM (`Patterns` donne les mêmes scores dans les deux modes). En effet, pitest remplace le bytecode de chaque mutant sans réinitialiser les champs statiques, donc les entrées du cache `SIMPLE_TYPES` créées pendant un mutant restent pour les suivants. Le mutant de la ligne 257 (`return null` remplacé par `return null`), identique au code d'origine, est ainsi déclaré tué. Nous ne savons pas quels sont les 8 mutants en trop, car le rapport final en JVM partagée n'a pas été conservé. Tous nos résultats utilisent donc un mutant par JVM.

### Mutants détectés grâce aux tests générés

`MediaType` : 19 mutants de plus que les tests d'origine. `Patterns` : 27.

| Classe | Ligne(s) | Mutation | Tué par | Pourquoi |
|---|---|---|---|---|
| `MediaType` | 103 (×2) | calcul de `semicolon` modifié (constructeur) | `application.testParseWithEmptyParameters` | `parse("application/json; ")` passe par le constructeur, qui calcule la position du point-virgule. Si elle est fausse, `getSubtype()` n'est plus `"json"`. |
| `MediaType` | 181, 189, 193, 197 | `application`, `image`, `text`, `video` retournent `null` | `testApplication`, `testImageWithValidType`, `testTextMethod`, `testVideoMethod` | Chaque test appelle la fabrique avec un sous-type valide (`application("json")`, `image("png")`, `text("plain")`, `video("mp4")`), donc un retour `null` fait échouer l'assertion. |
| `MediaType` | 277 | condition inversée (`parse`) | `image.testImageWithEmptyType` | `image("")` revient à `parse("image/")`. Avec la condition inversée, le code entre dans la branche « charset en premier » sans correspondance, et `matcher.group()` lève une exception. |
| `MediaType` | 288 (×2) | conditions inversées (`isSimpleName`) | `application.testParseInvalid` | `parse("application/json/")` a un `/` de trop que le chemin rapide doit refuser. Avec une condition inversée, `parse` retourne un type au lieu de `null`. |
| `MediaType` | 293 (×2) | `> 0` → `>= 0` ; retourne `true` (`isSimpleName`) | `image.testImageWithEmptyType` | Avec `image("")`, si la condition devient `>= 0` ou si le retour est toujours vrai, le nom vide est accepté et le résultat est `image/` au lieu de `null`. |
| `MediaType` | 379, 387 (×2) | `getType`, `getSubtype` retournent `""` ; `slash + 1` modifié | `application.testParseSimple` | `testParseSimple` compare `getType()` et `getSubtype()` à des valeurs exactes (`application/octet-stream`, `text/plain`), donc un retour `""` ou une sous-chaîne décalée fait échouer l'égalité. |
| `MediaType` | 398 | condition inversée (`hasParameters`) | `hasParameters.testHasParametersWithSpecialCharactersInBothKeyAndValue` | Les tests de `hasParameters` ne font que des `assertTrue` sur des types avec paramètres. Une condition inversée retourne `false` et l'assertion échoue. |
| `MediaType` | 416, 418 | condition inversée ; retourne `false` (`equals`) | `text.testParseMethod` | `assertEquals(TEXT_PLAIN, parse("text/plain"))` appelle `equals` sur deux types égaux, donc un `equals` qui retourne `false` fait échouer l'assertion. |
| `MediaType` | 418 | retourne `true` (`equals`) | `equals.testEqualsDifferentParameters` | `octet-stream` et `octet-stream; charset=UTF-8` ne diffèrent que par un paramètre, donc un `equals` qui retourne `true` fait échouer le `assertFalse`. |
| `MediaType` | 429 | retourne `0` (`compareTo`) | `compareTo.testCompareToDifferentType` | `compareTo` entre deux types différents doit être non nul. Le mutant retourne `0` et `assertNotEquals(0, ...)` échoue. |
| `Patterns` | 76, 90, 147, 148 | appel de `addName` retiré ; conditions inversées ; retourne `null` | `add_1.testAddExtension` | `add("jpg")`, sans `*`, `?` ni `[`, enregistre un nom exact (malgré le nom `testAddExtension`) que `matches("jpg")` doit retrouver. Si l'appel à `addName` est retiré, ou si l'enregistrement ou la recherche sont faussés, le type n'est plus retourné. |
| `Patterns` | 90, 92, 93 | conditions inversées (`addName`) | `add_1.testAddExtensionConflict` | `"jpg"` est enregistré pour `image/jpeg` puis `image/png`, ce qui crée un conflit de nom. Avec les conditions inversées, il passe inaperçu et la `MimeTypeException` n'est pas levée. |
| `Patterns` | 80, 102, 107, 108 | appel de `addExtension` retiré ; conditions inversées | `add_1.testAddGlobConflict` | `*.pdf` est une extension (malgré le nom `testAddGlobConflict`). L'enregistrer pour deux types doit lever `MimeTypeException`, ce qui n'arrive plus si l'appel à `addExtension` est retiré ou si ses contrôles sont inversés. |
| `Patterns` | 153 (×2), 154, 155, 156 | boucle sur les extensions modifiée ; retourne `null` (`matches`) | `add_1.testAddGlob`, `matches_5.testMatchesEmptyName` | `add("*.pdf")` puis `matches("*.pdf")` parcourt la boucle sur les extensions. Si ses bornes, son indice ou son retour sont faussés, le type n'est pas trouvé, et `matches("")` lève une exception au lieu de retourner `null`. |
| `Patterns` | 71, 117, 162, 163 | appel de `addGlob` retiré ; conditions inversées ; retourne `null` | `add_1.testAddGlobWithJavaRegex` | `add("(?i)\\.pdf$", true)` enregistre une expression régulière Java que `matches(".pdf")` doit retrouver. Si l'ajout est retiré (71), si le test de `null` est inversé (117, plantage au premier ajout) ou si la recherche par glob est faussée (162, 163), le type n'est plus retourné. |
| `Patterns` | 83, 173 (×2), 175, 177, 179, 187 | appel de `addGlob` retiré ; conversion glob → regex modifiée (`compile`) | `matches_5.testMatchesGlobMatch` | `add("example.*")` est un vrai glob. Il doit être stocké (83) puis converti en expression régulière (`?`, `*`, échappement, bornes de la boucle, résultat vide). Si une étape est faussée, `matches("example.txt")` ne retourne plus le type. |

## 6. Tests écrits à la main

[`MediaTypeHandWrittenTest.java`](tika-core/src/test/java/org/apache/tika/mime/MediaTypeHandWrittenTest.java) (12 méthodes, 14 exécutions) et [`PatternsHandWrittenTest.java`](tika-core/src/test/java/org/apache/tika/mime/PatternsHandWrittenTest.java) (2 méthodes). Parmi les mutants restés vivants après les tests générés, ils tuent 22 des 31 de `MediaType` et les 5 de `Patterns`.

| Test | Mutants tués | Intention | Données et motivation | Oracle |
|---|---|---|---|---|
| `audioFactoryProducesAudioWavType` | 185 | `audio` retourne un type et non `null`, alors qu'aucun test ne l'exécutait. | `"wav"`, car tout sous-type valide convient. | `toString()` égal à `"audio/wav"`. |
| `mediaTypeSetHasNoNullsOrDuplicates` | 211, 215 | `set(MediaType...)` ignore `null` et doublons et retourne un ensemble non modifiable (Javadoc). | `TEXT_PLAIN, null, application/json, TEXT_PLAIN`, soit un `null` et un doublon entre des types valides. | Taille 2, aucun élément `null`, et `add` lève `UnsupportedOperationException`. Ce test ne vérifie pas quels types l'ensemble contient. |
| `stringSetDropsEntriesWithEmptySubtype` | 230, 234 | `set(String...)` ignore les chaînes qui ne s'analysent pas. | `"text/plain"`, `"text/"`, `"application/json"`. `"text/"` a un sous-type vide et donne `null`. | Égalité avec l'ensemble des deux types valides. |
| `emptyTypeOrSubtypeIsRejected` | 259, 293 | `parse` refuse un type ou un sous-type vide. | `"/"` tue 293 et `"/plain"` (type vide, sous-type valide) tue 259, alors que `"text/"` seul ne tue ni l'un ni l'autre. | `assertNull` pour chaque entrée. |
| `charsetFirstFormIsParsedIntoTypeAndParameter` | 278 | La forme `charset=...; type/sous-type` (TIKA-350) est acceptée. Les lignes 277–278 n'étaient jamais exécutées. | `"charset=windows-1252; application/json"`, avec des valeurs arbitraires dans la forme attendue. | Non `null` (le mutant retourne `null`), type de base `application/json`, charset égal à `"windows-1252"`. |
| `asciiUpperCaseIsLowerCased` (×3) | 288 (2 mutants) | Les majuscules ASCII sont mises en minuscules, car les types sont insensibles à la casse (RFC 2045), et le chemin rapide doit les refuser. | `APPLICATION/JSON`, `Application/Json` et `application/JSON`, soit tout en majuscules, les initiales et le sous-type seul. | Égal à `parse("application/json")`. |
| `latinOneUpperCaseIsLowerCased` | 288 (1 mutant) | Une majuscule non ASCII (au-dessus de `'z'`) est aussi mise en minuscule. | `"ÄÖ/Ü"`, uniquement des lettres non ASCII, sinon le nom prend le chemin normal même avec le mutant. | `"äö/ü"` exact. |
| `charsetConstructorAddsCharsetParameter` | 350 | Ajouter un paramètre à un type qui n'en a pas avec le constructeur `MediaType(MediaType, Charset)`, jamais exécuté. | `TEXT_PLAIN` et `UTF_16`. Un seul constructeur suffit, car tous passent par `union`. | Égal à `parse("text/plain; charset=UTF-16")`, avec un seul paramètre. |
| `emptyMapKeepsExistingParameters` | 349, 352 | Ajouter une table vide ne fait pas perdre les paramètres existants. | `text/html; charset=UTF-8` et `Collections.emptyMap()`, car il faut un type avec paramètres. | `charset` vaut `"UTF-8"`, avec un seul paramètre. |
| `mergeKeepsOldAndNewParameters` | 351, 355, 356, 357 | Avec des paramètres des deux côtés, la fusion garde l'ancien et ajoute le nouveau. | `charset=UTF-8` existant et `format=flowed` ajouté, deux noms distincts. | Deux `get` aux valeurs exactes et une taille de 2. Un mutant perd l'ancien paramètre, un autre le nouveau. |
| `baseTypeStringHasNoParameters` | 367, 370, 398 | `getBaseType` retire les paramètres, et le résultat n'en signale aucun. | `application/json; charset=UTF-8`. Il faut des paramètres au départ, sinon 367 survit. | `toString()` égal à `"application/json"` et `assertFalse(hasParameters())`. Ce cas `false`, absent des tests générés, tue le mutant « toujours vrai » (398). |
| `equalsRejectsOtherKindsOfObject` | 420 | `equals` retourne `false` pour un objet qui n'est pas un `MediaType` (contrat d'`Object.equals`). | La `String` `"text/plain"` (même texte que `TEXT_PLAIN`) et `null`. | `assertNotEquals` avec le `MediaType` en premier argument (sinon c'est `String.equals` qui s'exécute), et `assertFalse(equals(null))`. |
| `exactNameWinsOverExtensionPattern` | `Patterns` 74, 75 | Un nom exact l'emporte sur un motif d'extension (Javadoc de `matches`). | `*.xml` pour `application/xml` et `pom.xml` pour `application/x-maven-pom`. Deux types distincts rendent le gagnant visible. Avec les mutants, `pom.xml` devient un glob, et les extensions passent avant les globs. | `assertSame` vérifie que `pom.xml` donne le type pom et `other.xml` le type xml. |
| `conflictingGlobWithUnrelatedTypeThrows` | `Patterns` 117, 119, 120 | Enregistrer le même glob pour deux types sans lien lève `MimeTypeException`. | `backup*.tar` (le `*` n'est pas en tête, donc c'est un glob et non une extension) pour `application/x-backup` puis `application/x-archive`, deux types qui n'étendent que `application/octet-stream`. Un type et son parent ne seraient pas en conflit. | `assertThrows(MimeTypeException.class, ...)`. |

**Mutants encore vivants : 9 dans `MediaType`.**

| Ligne | Méthode | Mutation | Pourquoi il survit |
|---|---|---|---|
| 254 | `parse` | condition inversée | Il ne décide que de la réutilisation de l'objet en cache. La méthode `parse` retourne un type égal mais une autre instance, que seul un `assertSame` sur deux appels détecterait (un tel test tuerait aussi 258 inversé, 259 et les quatre 288). Nous ne l'avons pas inclus, car la Javadoc de `parse` ne promet pas la même instance et le code présente le cache comme une optimisation (« Optimization for the common cases »), donc ce test vérifierait un détail d'implémentation. |
| 257 | `parse` | retourne `null` | Le mutant remplace `return null` par `return null`, donc le code est identique et le mutant est vraiment équivalent. pitest ne le voit pas, car un `return` dans un bloc `synchronized` passe par une variable temporaire. |
| 258 (×2) | `parse` | limite `SIMPLE_TYPES.size() < 10000` : borne modifiée ; condition inversée | Pour la borne `<` remplacée par `<=`, seule une taille de cache d'exactement 10 000 la distingue, ce qui exigerait de remplir le cache statique de la JVM. Avec la condition inversée, le type n'est plus mis en cache, et c'est le même cas que 254 (valeur égale, autre instance). |
| 259 | `parse` | `slash + 1` modifié | Avec `slash - 1`, la sous-chaîne vérifiée contient le `/`, donc le chemin rapide refuse le nom et le chemin normal retourne un type égal. C'est le même cas que 254, seule l'identité change. |
| 288 (×4) | `isSimpleName` | `<=` → `<` dans les bornes `'0'`, `'9'`, `'a'`, `'z'` | Aux bornes `'0'`, `'9'`, `'a'` et `'z'`, le chemin rapide refuse le caractère et le chemin normal donne le même résultat en minuscules. C'est le même cas que 254, seule l'identité change. |

## 7. GitHub Action

[`.github/workflows/tache2.yml`](.github/workflows/tache2.yml) : à chaque push et pull request sur `main`, Temurin JDK 25, `./mvnw -B install -pl :tika-core -am`. Elle exécute tous les tests de `tika-core`, dont nos 84 nouveaux tests : 68 générés (46 `MediaType`, 22 `Patterns`) et 16 écrits à la main (14 `MediaType`, 2 `Patterns`). Les workflows d'Apache Tika sont passés en exécution manuelle dans ce fork.

Exécution réussie : [n° 38191835771](https://github.com/LucBrennan/tika/actions/runs/38191835771) (833 tests de `tika-core`, 0 échec).

## 8. Déclaration d'utilisation de l'IA

- *Outils d'intelligence artificielle* : qwen2.5-coder:3b (équipe Qwen, Alibaba Cloud), exécuté localement avec Ollama 0.35.1 et utilisé par ChatUniTest 2.1.1 comme objet de l'étude, octobre 2026 ; Claude Opus 5.5 (Anthropic, claude.ai), octobre 2026 ; (Claude Opus 5.5, Anthropic), octobre 2026 ;
- *Méthodologie* : Claude a aidé à choisir les classes et à configurer pitest, Ollama et ChatUniTest, et a relevé dans le code de ChatUniTest les contraintes décrites à la section 2 ;
- *Méthode de collecte des données* : nous avons exécuté toutes les générations et mesures ; Claude Code a configuré pitest pour exécuter un mutant par JVM ;
- *Exécution* : Claude (claude.ai et Claude Code) a corrigé les tests générés et écrit la GitHub Action ; nous avons écrit les tests de la section 6 à partir de la liste des mutants survivants, analysée par Claude ;
- *Analyse des données* : Claude a associé les mutants aux lignes de code ;
- *Rédaction – Révision et édition* : Claude a proposé la structure de ce README et a rédigé les tableaux, les extraits de configuration, les commandes et les sections descriptives à partir de nos rapports ; nous avons rédigé les sections d'analyse (justification, critique des tests générés, comparaison des oracles, explication des mutants, documentation des tests) ; Claude les a traduites de l'anglais au français ;
- *Confidentialité et sécurité* : seul du code source libre a été partagé avec les outils, aucune donnée personnelle.
