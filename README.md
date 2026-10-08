# IFT3913 — Tâche 2 : tests générés par un LLM et analyse de mutation de `EndianUtils` (Apache Tika)

Ce dépôt est une copie (fork) d'[Apache Tika](https://github.com/apache/tika), dont le README d'origine est conservé dans [`README-apache-tika.md`](README-apache-tika.md). Ce README documente la tâche 2 en suivant, dans l'ordre, les étapes de l'énoncé.

## Sommaire

| Étape de l'énoncé | Section |
|---|---|
| Choisir une classe qui a déjà des tests, mais pas une couverture de 100 % | [1. Classe choisie](#1-classe-choisie) |
| Installer ChatUniTest avec un LLM ouvert exécuté localement | [2. ChatUniTest dans le pipeline Maven](#2-chatunitest-dans-le-pipeline-maven) |
| Générer des tests et documenter le résultat (où, compilation, corrections) | [3. Tests générés](#3-tests-générés) |
| Comparer les oracles de l'IA à ceux écrits à la main | [4. Oracles](#4-oracles--tests-générés-et-tests-écrits-à-la-main) |
| Ajouter PIT, calculer le score de mutation avec les tests originaux puis avec les tests générés, expliquer les mutants détectés | [5. Analyse de mutation avec PIT](#5-analyse-de-mutation-avec-pit) |
| Ajouter et documenter des tests pour les mutants non détectés | [6. Tests écrits à la main](#6-tests-écrits-à-la-main) |
| Exécuter les nouveaux tests dans une GitHub Action | [7. GitHub Action](#7-github-action) |
| Déclarer l'utilisation de l'IA | [8. Déclaration d'utilisation de l'IA](#8-déclaration-dutilisation-de-lia) |
| Bilan | [9. Conclusion](#9-conclusion--endianutils-avant-et-après) |

### Fichiers

| Chemin | Contenu |
|---|---|
| [`tika-core/pom.xml`](tika-core/pom.xml) | Configuration de PIT et de ChatUniTest (blocs « Tâche 2 ») |
| [`tika-core/chatunitest-tests/…/org/apache/tika/io/`](tika-core/chatunitest-tests/tika-parent/tika-core/org/apache/tika/io/) | Les 12 tests **bruts**, tels que ChatUniTest les a écrits |
| [`tika-core/src/test/java/org/apache/tika/io/EndianUtils_*_Test.java`](tika-core/src/test/java/org/apache/tika/io/) | Les mêmes tests, **intégrés** à Tika et corrigés |
| [`EndianUtilsMutationTest.java`](tika-core/src/test/java/org/apache/tika/io/EndianUtilsMutationTest.java), [`EndianUtilsCouvertureTest.java`](tika-core/src/test/java/org/apache/tika/io/EndianUtilsCouvertureTest.java) | Les tests écrits à la main |
| [`tache2/chatunitest-EndianUtils.log`](tache2/chatunitest-EndianUtils.log), [`tache2/chatunitest-info/`](tache2/chatunitest-info/) | Journal de la génération ; demandes envoyées au modèle, réponses et erreurs de chaque tentative |
| [`tache2/jacoco-1-tests-originaux/`](tache2/jacoco-1-tests-originaux/index.html) … [`jacoco-4-avec-tests-de-couverture/`](tache2/jacoco-4-avec-tests-de-couverture/index.html) | Rapports de couverture JaCoCo d'`EndianUtils` pour les quatre mesures ci-dessous |
| [`tache2/pit-1-tests-originaux/`](tache2/pit-1-tests-originaux/index.html) … [`pit-4-avec-tests-de-couverture/`](tache2/pit-4-avec-tests-de-couverture/index.html) | Rapports PIT (HTML et XML) pour les quatre mesures |
| [`.github/workflows/tache2.yml`](.github/workflows/tache2.yml) | La GitHub Action |

Les rapports portent sur quatre suites de tests, chacune comprenant la précédente :

1. **tests d'origine** : `EndianUtilsTest`, 4 tests écrits par les développeurs de Tika ;
2. **+ tests générés** par ChatUniTest : 22 tests (section 3) ;
3. **+ tests écrits à la main pour les mutants survivants** : 5 tests (section 6.1) ;
4. **+ tests écrits à la main pour les mutants non couverts** : 6 tests (section 6.2).

Sur GitHub, une page HTML s'affiche sous forme de code : pour voir les rapports, il faut cloner le dépôt (ou télécharger son archive) et ouvrir les fichiers `index.html` dans un navigateur. Dans les rapports JaCoCo, les lignes couvertes sont en vert, les lignes couvertes en partie (une partie des branches seulement) en jaune, et les autres en rouge. Les chiffres de ce README portent sur la classe `EndianUtils` ; la ligne « Total » des rapports JaCoCo compte aussi sa classe interne `BufferUnderrunException` (2 lignes, toujours couvertes).

## 1. Classe choisie

La classe étudiée est **`org.apache.tika.io.EndianUtils`**, du module `tika-core`. Elle lit des entiers en little-endian (LE), en big-endian (BE) ou en « middle-endian », depuis un flux (méthodes `read*`) ou depuis un tableau d'octets (méthodes `get*`). Elle a déjà des tests, `EndianUtilsTest`, mais ils sont loin de la couvrir entièrement :

| Avec les tests d'origine | Valeur |
|---|---|
| Lignes couvertes (JaCoCo) | 31 / 121 (25 %) |
| Branches couvertes | 10 / 28 (35 %) |
| Méthodes couvertes | 4 / 32 |
| Mutants PIT : tués / survivants / non couverts | 38 / 14 / 154, sur 206 |

Le [rapport JaCoCo](tache2/jacoco-1-tests-originaux/org.apache.tika.io/EndianUtils.java.html) montre en rouge les 90 lignes qu'aucun test d'origine n'exécute, et le [rapport PIT](tache2/pit-1-tests-originaux/index.html) les mutants vivants.

Pourquoi cette classe :

- **Ses tests d'origine sont minces.** Les 4 tests n'appellent que 4 de ses 31 méthodes publiques (`readUE7`, `readUIntLE`, `readUIntBE`, `readIntME`) : les 27 autres ne sont jamais exécutées.
- **Même le code testé a des mutants vivants.** 14 mutants survivent. Par exemple, dans `testReadUIntBE`, le cas du flux trop court appelle `readUIntLE` au lieu de `readUIntBE` ([`EndianUtilsTest.java`, ligne 68](tika-core/src/test/java/org/apache/tika/io/EndianUtilsTest.java#L68)) : la détection de fin de flux de `readUIntBE` n'est jamais vérifiée, et 4 mutants survivent à la ligne 111.
- **Ses résultats se calculent à la main.** Les méthodes sont statiques et sans état : on peut vérifier chaque oracle produit par le LLM.
- **Elle manipule des octets** (masques `& 0xFF`, décalages, signe), une source classique d'erreurs qui produit beaucoup de mutants arithmétiques.

## 2. ChatUniTest dans le pipeline Maven

ChatUniTest 2.1.1 est déclaré comme plugin Maven dans [`tika-core/pom.xml`](tika-core/pom.xml). Le LLM est **CodeQwen1.5-7B-Chat** (`codeqwen:v1.5-chat`), un modèle ouvert exécuté localement par **Ollama**, que ChatUniTest interroge par son API compatible OpenAI (`http://localhost:11434/v1/chat/completions`).

```xml
<plugin>
  <groupId>io.github.zju-aces-ise</groupId>
  <artifactId>chatunitest-maven-plugin</artifactId>
  <version>2.1.1</version>
  <configuration>
    <apiKeys>ollama</apiKeys>
    <model>codeqwen:v1.5-chat</model>
    <url>http://localhost:11434/v1/chat/completions</url>
    <testNumber>1</testNumber>
    <maxRounds>3</maxRounds>
    <maxPromptTokens>3000</maxPromptTokens>
    <maxResponseTokens>1024</maxResponseTokens>
    <temperature>0.2</temperature>
    <thread>false</thread>
    <merge>false</merge>
    <tmpOutput>${project.build.directory}/chatunitest-info</tmpOutput>
    <testOutput>${project.basedir}/chatunitest-tests</testOutput>
  </configuration>
</plugin>
```

| Option | Raison |
|---|---|
| `apiKeys` | ChatUniTest exige une clé non vide ; Ollama l'ignore. |
| `model` | Nom reconnu à la fois par ChatUniTest, qui n'accepte qu'une liste fermée de modèles, et par Ollama. |
| `testNumber`, `maxRounds` | Un test par méthode, avec au plus deux tours de réparation, pour garder une durée raisonnable sur un ordinateur portable. |
| `temperature` | Une valeur basse donne des réponses plus stables. |
| `thread=false` | Un LLM local traite les demandes une par une. En parallèle, elles s'accumulent et dépassent le délai HTTP de 5 minutes de ChatUniTest. |
| `merge=false` | La classe « suite » que ChatUniTest fusionne utilise `@RunWith(JUnitPlatform.class)`, de JUnit 4, absent de Tika, qui utilise JUnit 6. |
| `tmpOutput`, `testOutput` | Les fichiers de travail vont dans `target/` ; les tests produits vont dans `tika-core/chatunitest-tests/`, hors des sources de Tika. |

ChatUniTest demande aussi la dépendance `chatunitest-starter` pour compiler et exécuter ses tests. Elle apporte de vieilles versions de Mockito, de ByteBuddy, le moteur JUnit Vintage et `junit-platform-runner`, incompatibles avec Java 17 et refusées par la règle `dependencyConvergence` de Maven Enforcer. Elles sont donc exclues, au profit des versions que gère Tika :

```xml
<dependency>
  <groupId>org.mockito</groupId>
  <artifactId>mockito-core</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.mockito</groupId>
  <artifactId>mockito-junit-jupiter</artifactId>
  <version>${mockito-junit-jupiter.version}</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>io.github.ZJU-ACES-ISE</groupId>
  <artifactId>chatunitest-starter</artifactId>
  <version>1.4.0</version>
  <type>pom</type>
  <scope>test</scope>
  <exclusions>
    <exclusion><groupId>org.mockito</groupId><artifactId>*</artifactId></exclusion>
    <exclusion><groupId>net.bytebuddy</groupId><artifactId>*</artifactId></exclusion>
    <exclusion><groupId>org.junit.vintage</groupId><artifactId>*</artifactId></exclusion>
    <exclusion><groupId>org.junit.platform</groupId><artifactId>junit-platform-runner</artifactId></exclusion>
  </exclusions>
</dependency>
```

Génération des tests de la classe entière :

```
ollama pull codeqwen:v1.5-chat
.\mvnw.cmd -pl tika-core chatunitest:class -DselectClass=EndianUtils
```

## 3. Tests générés

### 3.1 Résultat de la génération

Pour chaque méthode, ChatUniTest envoie au modèle la méthode et le contexte de sa classe, extrait le code Java de la réponse, le compile, puis l'exécute. En cas d'erreur, il renvoie le message au modèle et lui demande de corriger, jusqu'à 3 tours. La génération des 31 méthodes a pris 4 h 03 (72 appels au modèle).

| Résultat | Méthodes | Nombre |
|---|---|---|
| Test produit au premier tour | `getShortLE(byte[], int)`, `getUShortLE` ×2, `getShortBE(byte[], int)`, `getUShortBE` ×2, `getUIntLE(byte[], int)`, `getUIntBE(byte[], int)`, `ubyteToInt`, `getUByte` | 10 |
| Test produit après réparation | `getIntBE(byte[])` au 2e tour, `getShortBE(byte[])` au 3e tour | 2 |
| Aucun test après 3 tours | les 12 méthodes `read*`, `getShortLE(byte[])`, `getIntLE` ×2, `getIntBE(byte[], int)`, `getUIntLE(byte[])`, `getUIntBE(byte[])`, `getLongLE` | 19 |

Sur 21 méthodes ratées au premier tour, 2 seulement ont été rattrapées par la réparation automatique. Erreur du dernier tour pour les 19 méthodes sans test, d'après [`tache2/chatunitest-info/error-message/`](tache2/chatunitest-info/error-message/) :

| Cause | Méthodes | Nombre |
|---|---|---|
| `BufferUnderrunException` introuvable : le modèle l'importe depuis `org.apache.tika.exception` ou `org.apache.tika.io`, alors que c'est une classe interne, `EndianUtils.BufferUnderrunException` | `readShortLE`, `readShortBE`, `readUShortLE`, `readUShortBE`, `readUIntLE`, `readUIntBE`, `readIntLE`, `readIntBE`, `readIntME`, `readLongBE` | 10 |
| Même exception, vérifiée mais ni attrapée ni déclarée (`throws`) | `readLongLE` | 1 |
| Valeur supérieure à 127 dans un `byte[]` sans conversion : `new byte[] {0xFF}` ne compile pas, car un octet Java est signé | `readUE7`, `getIntLE(byte[], int)`, `getIntBE(byte[], int)`, `getUIntLE(byte[])`, `getUIntBE(byte[])`, `getLongLE` | 6 |
| Variable non finale utilisée dans une lambda | `getShortLE(byte[])` | 1 |
| Le test plante à l'exécution (`ArrayIndexOutOfBoundsException` : tableau trop court) | `getIntLE(byte[])` | 1 |

Pour `getUIntBE(byte[])`, les deux tours de réparation n'ont même pas fourni de code exploitable. Ailleurs, le modèle recevait bien le message du compilateur, mais ne savait pas en tirer la correction : pour `BufferUnderrunException`, il a essayé d'autres paquets sans jamais trouver la classe interne.

### 3.2 Où sont les tests générés

- **Version brute**, telle que ChatUniTest l'a écrite : [`tika-core/chatunitest-tests/tika-parent/tika-core/org/apache/tika/io/`](tika-core/chatunitest-tests/tika-parent/tika-core/org/apache/tika/io/). Elle est conservée pour comparaison, et le build ne la compile pas.
- **Version intégrée** à la suite de Tika : `tika-core/src/test/java/org/apache/tika/io/EndianUtils_<méthode>_<numéro>_0_Test.java`, 12 classes et 22 méthodes de test. Le numéro est l'indice de la méthode testée dans `EndianUtils`, ce qui distingue les surcharges.

### 3.3 Compilent-ils et s'exécutent-ils sans intervention ?

**Dans ChatUniTest, oui en apparence.** Les 12 tests compilent, et ChatUniTest affiche « compile and execute successfully » pour chacun. Pourtant, sa propre exécution JUnit montre 6 méthodes de test en échec, dans 5 fichiers. ChatUniTest ne rejette un test que s'il ne compile pas ou s'il lève une exception inattendue. Une assertion qui échoue, c'est-à-dire un oracle faux, est acceptée.

**Dans le build de Tika, non.** Checkstyle s'exécute avant la compilation (phase `validate`) et fait échouer le build. Chaque fichier viole au moins trois de ses règles :

- l'en-tête de licence Apache est absent (`RegexpHeader`) ;
- il contient des `import ...*` (`AvoidStarImport`) ;
- il a des imports inutiles (`UnusedImports`) : Mockito, `ExtendWith`, `TikaException`, `IOException` et `InputStream`, que ChatUniTest ajoute à tous les tests.

**Corrections de forme, automatiques, sur les 12 fichiers :**

1. Remplacer les imports `*` par des imports précis, en une commande :

   ```
   powershell -NoProfile -Command "Get-ChildItem tika-core\src\test\java\org\apache\tika\io\EndianUtils_*_Test.java | ForEach-Object { $t = [IO.File]::ReadAllText($_.FullName); $t = $t.Replace('import static org.mockito.Mockito.*;', '').Replace('import org.mockito.*;', '').Replace('import org.junit.jupiter.api.*;', 'import org.junit.jupiter.api.Test;').Replace('import static org.junit.jupiter.api.Assertions.*;', 'import static org.junit.jupiter.api.Assertions.assertEquals;' + [char]10 + 'import static org.junit.jupiter.api.Assertions.assertThrows;'); [IO.File]::WriteAllText($_.FullName, $t) }"
   ```

2. Lancer l'outil de formatage de Tika, `.\mvnw.cmd -pl tika-core spotless:apply`. Il ajoute l'en-tête de licence, supprime les imports inutiles, trie les imports et passe les fins de ligne au format Unix.

Après ces deux étapes, Checkstyle ne signale plus aucune violation, et les 22 méthodes de test s'exécutent : 16 passent et 6 échouent, les mêmes que dans ChatUniTest.

**Corrections de fond, à la main : 6 lignes dans 5 fichiers.** Chaque valeur a été vérifiée en exécutant `EndianUtils`.

| Test | Problème | Correction | Justification |
|---|---|---|---|
| `EndianUtils_getUShortBE_19_0_Test.testGetUShortBE` | Oracle faux : attend `0x0201` | `0x0203` | Les octets 2 et 3 de `{0, 1, 2, …, 7}` valent `0x02` et `0x03`. |
| `EndianUtils_getUIntLE_25_0_Test.testGetUIntLE` | Oracle faux : lit `{0, 0, 0, 1}` comme du big-endian et attend `1` | `0x01000000L` | En little-endian, le dernier octet est celui de poids fort. |
| `EndianUtils_ubyteToInt_29_0_Test.testUbyteToInt` | Oracle faux : attend `0xF2` pour `(byte) -34` | `0xDE` | -34 sans signe vaut 256 − 34 = 222 = `0xDE`. Le modèle a confondu avec -14, qui vaut `0xF2`. |
| `EndianUtils_getShortBE_16_0_Test.testGetShortBE` | Test contradictoire : après avoir vérifié le résultat, il exige une exception sur le même tableau valide | `getShortBE(new byte[] { 0 })` | Le commentaire du test annonce « un tableau invalide » : avec un seul octet, la lecture déborde bien. |
| `EndianUtils_getShortBE_17_0_Test.testGetShortBEWithNullData` | Appel par réflexion : la `NullPointerException` attendue arrive enveloppée dans une `InvocationTargetException` | Appel direct `EndianUtils.getShortBE(data, offset)` | La méthode est publique : la réflexion était inutile. |
| `EndianUtils_getShortBE_17_0_Test.testGetShortBEWithOffsetOutOfBounds` | Même problème avec `ArrayIndexOutOfBoundsException` | Appel direct | Même raison. |

**Bilan : aucun test n'a été intégrable sans intervention.** Les 12 fichiers ont demandé les mêmes corrections de forme, automatisables. 6 méthodes de test sur 22 ont demandé une correction de fond d'une ligne : 3 oracles faux, 1 donnée d'entrée contradictoire et 2 appels par réflexion. Après ces corrections, les 22 tests passent.

## 4. Oracles : tests générés et tests écrits à la main

Ici, les tests écrits à la main sont ceux d'origine, `EndianUtilsTest`, écrits par les développeurs de Tika.

| | Tests écrits à la main | Tests générés par ChatUniTest |
|---|---|---|
| Méthodes testées | 4 méthodes `read*` | 11 méthodes appelées directement (des `get*`, `ubyteToInt`, `getUByte`) et 2 autres par délégation ; aucune méthode `read*` |
| Type d'oracle | Valeurs exactes, tirées du format : par exemple, un exemple documenté pour le middle-endian | Valeurs exactes, calculées de tête par le modèle ; exceptions |
| Choix des données | Octets de poids fort (`0xF0`, `0xFF`), qui vérifient le traitement non signé (`4294967280L`) | Souvent des octets nuls (`{0, 0, 0, 1}`) ou de petites valeurs (`0x12`, `0x34`) |
| Exceptions vérifiées | `BufferUnderrunException`, le contrat documenté par `@throws` | `NullPointerException` et `ArrayIndexOutOfBoundsException`, un comportement que la documentation ne promet pas |
| Erreurs | 1 copier-coller (`testReadUIntBE` appelle `readUIntLE`) | 3 valeurs fausses, 1 test contradictoire, 2 mauvais usages de la réflexion : 6 méthodes sur 22 |
| Style | Une méthode de test par méthode testée, avec plusieurs cas | Réflexion inutile dans 4 classes ; mauvaise surcharge testée dans `getUShortLE_14` ; imports inutiles dans toutes les classes |

Ce qu'on en retient :

- **Quand ils sont justes, les oracles générés sont forts.** Ce sont des `assertEquals` sur des valeurs exactes, et 16 méthodes de test sur 22 sont correctes. Certains choix sont bons : `getIntBE_22` place un 1 à chaque position tour à tour, puis teste `0xFF` partout, ce qui tue tous les mutants non équivalents de `getIntBE`.
- **Mais 3 oracles sur 22 sont faux** : une erreur de calcul, une confusion LE/BE et une erreur de complément à deux. Comme ChatUniTest les présente comme réussis, une relecture humaine reste indispensable : un oracle faux ne détecte aucun bogue, il en signale un qui n'existe pas.
- **Les données choisies par le modèle détectent moins bien les erreurs arithmétiques.** Avec des octets nuls, un décalage ou une soustraction faussés ne changent pas le résultat : 5 mutants de `getIntLE` survivent pour cette raison (section 5.4).
- **Les deux suites se complètent.** Les tests écrits à la main couvrent les méthodes `read*`, où le LLM a échoué. Les tests générés couvrent les méthodes `get*`, que personne ne testait.

## 5. Analyse de mutation avec PIT

### 5.1 Configuration

PIT 1.30.0 est ajouté à [`tika-core/pom.xml`](tika-core/pom.xml) :

```xml
<plugin>
  <groupId>org.pitest</groupId>
  <artifactId>pitest-maven</artifactId>
  <version>1.30.0</version>
  <dependencies>
    <dependency>
      <groupId>org.pitest</groupId>
      <artifactId>pitest-junit5-plugin</artifactId>
      <version>1.2.3</version>
    </dependency>
  </dependencies>
  <configuration>
    <targetClasses>
      <param>org.apache.tika.io.EndianUtils*</param>
    </targetClasses>
    <targetTests>
      <param>org.apache.tika.*</param>
    </targetTests>
    <outputFormats>
      <outputFormat>HTML</outputFormat>
      <outputFormat>XML</outputFormat>
    </outputFormats>
    <timestampedReports>false</timestampedReports>
    <parseSurefireArgLine>false</parseSurefireArgLine>
  </configuration>
</plugin>
```

- `pitest-junit5-plugin` permet à PIT d'exécuter les tests de la plateforme JUnit, qu'utilise Tika (JUnit 6).
- Le motif `EndianUtils*` inclut la classe interne `BufferUnderrunException`.
- Les mutateurs sont ceux de PIT par défaut : limites des conditions, incréments, négation des conditions, opérateurs arithmétiques et binaires, valeurs de retour, appels supprimés.
- `parseSurefireArgLine=false` est nécessaire : la ligne de commande de Surefire, dans Tika, contient `@{surefireArgLine}` (l'agent JaCoCo), que PIT ne sait pas interpréter.

Commandes, depuis la racine du dépôt (sous Linux ou macOS, remplacer `.\mvnw.cmd` par `./mvnw`) :

```
:: Compiler tika-core et exécuter tous ses tests
.\mvnw.cmd -pl tika-core -am install

:: Mesure 1 (tests d'origine) : exclure tous les tests ajoutés
.\mvnw.cmd -pl tika-core org.pitest:pitest-maven:mutationCoverage -DexcludedTestClasses=org.apache.tika.io.EndianUtils_*_Test,org.apache.tika.io.EndianUtilsMutationTest,org.apache.tika.io.EndianUtilsCouvertureTest

:: Mesure 2 (+ tests générés) : exclure les tests écrits à la main
.\mvnw.cmd -pl tika-core org.pitest:pitest-maven:mutationCoverage -DexcludedTestClasses=org.apache.tika.io.EndianUtilsMutationTest,org.apache.tika.io.EndianUtilsCouvertureTest

:: Mesures 3 et 4 : exclure seulement EndianUtilsCouvertureTest, puis aucun test
.\mvnw.cmd -pl tika-core org.pitest:pitest-maven:mutationCoverage -DexcludedTestClasses=org.apache.tika.io.EndianUtilsCouvertureTest
.\mvnw.cmd -pl tika-core org.pitest:pitest-maven:mutationCoverage
```

Le rapport est écrit dans `tika-core\target\pit-reports\index.html`.

### 5.2 Score de mutation avec les tests originaux, puis avec les tests générés

| Mesure | Tués | Survivants | Non couverts | Score de mutation | Force des tests | Rapport |
|---|---|---|---|---|---|---|
| 1. Tests d'origine | 38 | 14 | 154 | 18 % (38/206) | 73 % | [`pit-1-tests-originaux`](tache2/pit-1-tests-originaux/index.html) |
| 2. + tests générés | 86 | 21 | 99 | 42 % (86/206) | 80 % | [`pit-2-avec-tests-generes`](tache2/pit-2-avec-tests-generes/index.html) |

Le *score de mutation* rapporte les mutants tués au total des mutants ; la *force des tests* les rapporte aux seuls mutants exécutés par au moins un test.

Les tests générés tuent 48 mutants de plus, mais **ils ne détectent pas tous les mutants** : 21 survivent et 99 ne sont exécutés par aucun test (section 5.4).

### 5.3 Mutants détectés grâce aux tests générés, et pourquoi

Les 48 mutants que les tests générés tuent en plus étaient tous **non couverts** auparavant : aucun test d'origine n'appelait ces méthodes. La colonne « Tués par » donne tous les tests générés qui tuent ces mutants, d'après la matrice complète de PIT (`mutations.xml`).

| Méthode | Lignes | Mutants tués | Tués par | Pourquoi |
|---|---|---|---|---|
| `getShortLE(byte[], int)` | 270 | Valeur de retour remplacée par 0 (1) | `getShortLE_13` | Le test attend `0x3412`. |
| `getUShortLE(byte[], int)` | 291–293 | Masques `& 0xFF` changés en `\| 0xFF` (2), `offset + 1` changé en `offset - 1`, décalage `<<` changé en `>>`, addition changée en soustraction, retour 0 (6) | `getUShortLE_14`, `getUShortLE_15`, `getShortLE_13` (par délégation) | Avec les octets `0x12` et `0x34` de `getShortLE_13`, tous deux non nuls, chaque opération compte dans le résultat. |
| `getShortBE(byte[])` | 303 | Retour 0 (1) | `getShortBE_16` | Le test attend 1. |
| `getShortBE(byte[], int)` | 314 | Retour 0 (1) | `getShortBE_17`, `getShortBE_16` (par délégation) | `getShortBE_17` attend -1 pour `{0xFF, 0xFF}`. |
| `getUShortBE(byte[])` | 324 | Retour 0 (1) | `getUShortBE_18` | Le test attend `0x1234`. |
| `getUShortBE(byte[], int)` | 335–337 | Mêmes 6 mutants que `getUShortLE` (6) | `getUShortBE_18`, `getUShortBE_19`, `getShortBE_17` et, pour 5 d'entre eux, `getShortBE_16` | Même raison, par exemple avec `{0x12, 0x34}` dans `getUShortBE_18`. |
| `getIntLE(byte[], int)` | 359–363 | `i++` changé en `i--` sur les 3 premières lectures, masques (4), décalage de l'octet de poids fort, retour 0 (9) | `getUIntLE_25` (oracle corrigé) | Décaler la position de lecture ou remplacer un masque change l'octet lu ; `1 << 24` devient 0 avec `>>`. |
| `getIntBE(byte[])` | 373 | Retour 0 (1) | `getIntBE_22` | |
| `getIntBE(byte[], int)` | 385–389 | `i++` (3), masques (4), décalages (3), additions (3), retour 0 (14) | `getIntBE_22` (les 14), `getUIntBE_27` (9) | `getIntBE_22` place un 1 à chaque position tour à tour : chaque décalage et chaque addition compte. |
| `getUIntLE(byte[], int)` | 411 | Masque `0xFFFFFFFF` changé en OU, retour 0 (2) | `getUIntLE_25` (oracle corrigé) | Le OU avec `0xFFFFFFFF` donne `0xFFFFFFFF` au lieu de `0x01000000`. |
| `getUIntBE(byte[], int)` | 433 | Mêmes 2 mutants (2) | `getUIntBE_27` | |
| `ubyteToInt(byte)` | 461 | `& 0xFF` changé en `\| 0xFF`, retour 0 (2) | `ubyteToInt_29` (oracle corrigé) | `0x12 \| 0xFF` vaut `0xFF` et non `0x12`. |
| `getUByte(byte[], int)` | 472 | Mêmes 2 mutants (2) | `getUByte_30` | Le test attend 3. |

14 de ces 48 mutants ne sont tués que par des tests corrigés à la main (section 3.3) : `getUIntLE_25` (11), `ubyteToInt_29` (2) et `getShortBE_16` (1). Sans correction, ces tests échouaient sur le code d'origine, et PIT ne pouvait pas s'en servir.

### 5.4 Mutants que les tests générés ne détectent pas

**21 mutants survivent** : les 14 des tests d'origine, que les tests générés ne touchent pas puisqu'ils n'appellent aucune méthode `read*`, et 7 nouveaux.

| Méthode (ligne) | Mutants | Pourquoi ils survivent |
|---|---|---|
| `readUIntLE` (92) | `< 0` changé en `<= 0` ; 2 des 3 OU changés en ET | Aucun test ne lit quatre octets nuls. La fin de flux n'est testée qu'au 4e octet, et un -1 en 4e position suffit à rendre le résultat négatif, même si un OU devient un ET. |
| `readUIntBE` (111) | `< 0` changé en `<= 0` ; les 3 OU changés en ET | Mêmes raisons ; de plus, le cas du flux trop court n'est jamais exécuté, à cause du copier-coller de `testReadUIntBE`. |
| `readIntME` (168) | `< 0` changé en `<= 0` ; 2 OU changés en ET | Comme `readUIntLE`. |
| `readUE7` (235) | `>= 0` changé en `> 0` ; `read++ < max` changé en `<=` ; `read++` changé en `read--` | Aucun test ne contient d'octet `0x00`, ni de valeur codée sur plus de 6 octets. |
| `readUE7` (246) | `i < 0` changé en `i <= 0` | Aucun test ne se termine par un octet `0x00`. |
| `getIntLE` (363) | 2 décalages, 3 additions | Seul le test de `getUIntLE` appelle cette méthode, avec `{0, 0, 0, 1}` : décaler ou soustraire un octet nul ne change rien. |
| `getIntLE` (362), `getIntBE` (388) | Dernier `i++` changé en `i--` | Mutants équivalents (section 6.3). |

**99 mutants ne sont exécutés par aucun test.** Ils se trouvent dans 14 méthodes que ni les tests d'origine ni les tests générés n'appellent, surtout les méthodes `read*` où ChatUniTest a échoué (section 3.1) :

| Méthodes | Mutants non couverts |
|---|---|
| `readShortLE`, `readShortBE` | 1 + 1 |
| `readUShortLE`, `readUShortBE` | 6 + 6 |
| `readIntLE`, `readIntBE` | 12 + 12 |
| `readLongLE`, `readLongBE` | 24 + 24 |
| `getLongLE` | 8 |
| `getShortLE(byte[])`, `getUShortLE(byte[])`, `getIntLE(byte[])`, `getUIntLE(byte[])`, `getUIntBE(byte[])` | 5 × 1 |

## 6. Tests écrits à la main

Des tests ont été écrits à la main pour tous les mutants non détectés, sauf les 2 qui sont équivalents (section 6.3). Les deux classes de tests utilisent deux méthodes utilitaires : `flux(...)` donne un flux qui contient exactement les octets indiqués ; `fluxScripte(...)` donne un flux qui renvoie les valeurs indiquées telles quelles, -1 compris, puis -1.

### 6.1 Pour les 21 mutants survivants : `EndianUtilsMutationTest`

#### `quatreOctetsNulsNeSontPasUneFinDeFlux`
- **Intention** : un octet de valeur 0 ne doit pas être pris pour une fin de flux.
- **Données** : quatre octets nuls, lus par `readUIntLE`, `readUIntBE` et `readIntME`. Le OU des quatre octets vaut alors exactement 0, la limite entre une lecture valide (≥ 0) et une fin de flux (< 0).
- **Oracle** : la valeur 0, sans exception.
- **Mutants tués** : `< 0` changé en `<= 0` aux lignes 92, 111 et 168 (3).

#### `uneFinDeFluxSuivieDOctetsEstDetectee`
- **Intention** : chacune des quatre lectures doit être vérifiée, et pas seulement la dernière. Pour `readUIntBE`, c'est aussi le cas du flux trop court que le test d'origine voulait vérifier, mais qu'il appliquait par erreur à `readUIntLE` (section 1).
- **Données** : un flux qui renvoie -1, puis 1, 2 et 3. Seule la première lecture échoue, comme sur une console après Ctrl-Z, dont la fin n'est pas définitive. Avec un flux ordinaire, toutes les lectures suivantes renverraient aussi -1, et la dernière suffirait à révéler la fin du flux.
- **Oracle** : `BufferUnderrunException` pour `readUIntLE`, `readUIntBE` et `readIntME`, l'exception que ces méthodes documentent pour un flux trop court.
- **Mutants tués** : les deux premiers OU changés en ET aux lignes 92 et 168, et les trois OU de la ligne 111 (7). Avec un ET, le -1 est combiné à un octet valide : `-1 & 1` vaut 1, et le résultat n'est plus négatif. Le troisième OU des lignes 92 et 168 était déjà tué par les tests d'origine.

#### `readUE7AccepteUnDernierGroupeNul`
- **Intention** : un dernier groupe de valeur 0 est valide.
- **Données** : `0x81 0x00`. `0x81` est un groupe de valeur 1 avec le bit de continuation ; `0x00` est le dernier groupe, nul, et se trouve à la limite des comparaisons `>= 0` et `< 0`.
- **Oracle** : 1 × 128 + 0 = 128.
- **Mutants tués** : `>= 0` changé en `> 0` (ligne 235) et `i < 0` changé en `i <= 0` (ligne 246) (2).

#### `readUE7NeDecodePasPlusDeSixOctets`
- **Intention** : vérifier la limite de six groupes fixée par le code (`max = 6`).
- **Données** : `0x81` suivi de six `0x80`, puis `0x00` : une valeur codée sur plus de six octets.
- **Oracle** : 2³⁵, soit un groupe de valeur 1 suivi de cinq groupes nuls. Le septième octet est lu, mais ignoré. Aucune documentation ne dit ce qui doit arriver au-delà de six octets : ce test fige le comportement actuel.
- **Mutants tués** : `read++ < max` changé en `<=` et `read++` changé en `read--` (ligne 235) (2).

#### `getIntLEAvecQuatreOctetsDistinctsNonNuls`
- **Intention** : chaque octet doit compter dans le résultat, avec son propre poids.
- **Données** : `{1, 2, 3, 4}`, quatre octets distincts et non nuls, là où le test généré utilisait `{0, 0, 0, 1}`.
- **Oracle** : `0x04030201` ; en little-endian, le dernier octet est celui de poids fort.
- **Mutants tués** : 2 décalages et 3 additions à la ligne 363 (5).

**Résultat (mesure 3)** : PIT tue 105 mutants ; les 19 survivants qui n'étaient pas équivalents sont tués ([`pit-3-avec-tests-manuels`](tache2/pit-3-avec-tests-manuels/index.html)).

### 6.2 Pour les 99 mutants non couverts : `EndianUtilsCouvertureTest`

Ces 6 tests appellent les 14 méthodes qu'aucun test n'exécutait, avec les mêmes principes : des octets distincts et non nuls, pour que chaque opération compte dans le résultat, et des valeurs à la limite des comparaisons.

#### `lectureDOctetsDistinctsDepuisUnFlux`
- **Intention** : les 8 méthodes de lecture jamais appelées (`readShortLE`, `readShortBE`, `readUShortLE`, `readUShortBE`, `readIntLE`, `readIntBE`, `readLongLE`, `readLongBE`) placent chaque octet à sa place, selon l'ordre LE ou BE.
- **Données** : les octets 1, 2, 3… Ils sont distincts et non nuls : un décalage dans le mauvais sens, une soustraction à la place d'une addition ou un octet mal placé change forcément le résultat. Avec des octets nuls, comme dans certains tests générés, ces erreurs passent inaperçues (section 4).
- **Oracle** : la valeur se lit directement en hexadécimal. En LE, le premier octet lu est celui de poids faible : `1, 2` donne `0x0201`. En BE, c'est celui de poids fort : `0x0102`. De même, `1, 2, …, 8` donne `0x0807060504030201` en LE et `0x0102030405060708` en BE.
- **Mutants tués** : tous les décalages, toutes les additions et toutes les valeurs de retour de ces 8 méthodes (52).

#### `octetsNulsLusSansFinDeFlux`
- **Intention** : un octet de valeur 0 n'est pas une fin de flux. C'est le comportement vérifié par `quatreOctetsNulsNeSontPasUneFinDeFlux` (section 6.1), ici pour `readUShortLE`, `readUShortBE`, `readIntLE`, `readIntBE`, `readLongLE` et `readLongBE`.
- **Données** : que des octets nuls. Le OU des octets lus vaut alors exactement 0, la limite entre une lecture valide (≥ 0) et une fin de flux (< 0).
- **Oracle** : la valeur 0, sans exception.
- **Mutants tués** : `< 0` changé en `<= 0` aux lignes 64, 73, 130, 149, 191 et 217 (6).

#### `finDeFluxDetecteeMemeSiDesOctetsSuivent`
- **Intention** : chacune des lectures doit être vérifiée, et pas seulement la dernière, comme dans `uneFinDeFluxSuivieDOctetsEstDetectee` (section 6.1), pour les mêmes 6 méthodes.
- **Données** : un flux qui renvoie -1, puis des octets valides (1, 2, 3…). Seule la première lecture échoue.
- **Oracle** : `BufferUnderrunException`, l'exception que ces méthodes déclarent pour un flux qui ne fournit pas assez d'octets.
- **Mutants tués** : chaque OU changé en ET dans les conditions de fin de flux : 1 + 1 (`readUShort*`), 3 + 3 (`readInt*`) et 7 + 7 (`readLong*`), soit 22. Avec un ET, le -1 est combiné à un octet valide : `-1 & 1` vaut 1, et le résultat n'est plus négatif.

La négation de ces 6 conditions (`< 0` changé en `>= 0`) inverse le test : l'exception est levée sur un flux valide, et ne l'est plus sur un flux tronqué. Ces 6 mutants sont tués par chacun des trois tests précédents.

#### `readUE7SignaleUnFluxTronque`
- **Intention** : `readUE7` doit signaler un flux qui s'arrête au milieu d'une valeur.
- **Données** : un flux vide, puis un flux qui ne contient que `0x81`. Le bit de poids fort de `0x81` annonce un octet suivant, qui n'arrive jamais.
- **Oracle** : une `IOException`. La méthode déclare cette exception et la lève avec le message « Buffer underun; expected one more byte » : une valeur incomplète ne doit pas être renvoyée comme si elle était valide.
- **Mutants tués** : aucun que les autres tests ne tuent déjà. Les 5 mutants qu'il tue, comme la négation de `i < 0` (ligne 246), sont aussi tués par `EndianUtilsTest.testReadUE7`. Il sert à la couverture : il est le seul à exécuter la ligne 247 et les deux branches de fin de flux de `readUE7`, que JaCoCo signalait encore comme non couvertes après la mesure 3. Vérification faite à la main : si on supprime le test `if (i < 0)`, ce test échoue, et c'est le seul.

#### `surchargesSansDecalageLisentDepuisLeDebut`
- **Intention** : les 5 surcharges sans décalage jamais appelées (`getShortLE(byte[])`, `getUShortLE(byte[])`, `getIntLE(byte[])`, `getUIntLE(byte[])`, `getUIntBE(byte[])`) lisent à partir de l'indice 0.
- **Données** : `{1, 2, 3, 4}`, quatre octets distincts.
- **Oracle** : la même valeur qu'avec un décalage de 0 : `0x0201` sur 2 octets en LE, `0x04030201` sur 4 octets en LE, `0x01020304` en BE.
- **Mutants tués** : la valeur de retour remplacée par 0, dans chacune des 5 surcharges (5). Elles ne font qu'appeler la version avec décalage, déjà testée : c'est leur seul mutant.

#### `getLongLEAvecOctetsDistinctsEtDecalage`
- **Intention** : `getLongLE` assemble 8 octets en little-endian, à partir du décalage donné.
- **Données** : huit octets distincts `{1, …, 8}`, d'abord au début du tableau, puis précédés de deux octets `0x7F` qui doivent être ignorés (décalage de 2). Le tableau a exactement la taille nécessaire : une lecture en dehors des 8 octets lève une exception.
- **Oracle** : `0x0807060504030201` dans les deux cas.
- **Mutants tués** : les 8 mutants de `getLongLE` : les bornes de la boucle (`>=` changé en `>`, condition niée, `offset + LONG_SIZE - 1` changé en `offset - LONG_SIZE - 1` ou en `offset + LONG_SIZE + 1`), le décalage `<<=` changé en `>>=`, le masque `0xff &` changé en `0xff |`, `|=` changé en `&=`, et la valeur de retour (8). PIT ne crée pas de mutant `j--` changé en `j++`, qui pourrait rendre la boucle infinie.

**Résultat (mesure 4)** : PIT tue 204 mutants sur 206 ; il ne reste aucun mutant non couvert ([`pit-4-avec-tests-de-couverture`](tache2/pit-4-avec-tests-de-couverture/index.html)).

### 6.3 Mutants encore vivants

**2 mutants survivent, et ils sont équivalents.** Aux lignes 362 (`getIntLE`) et 388 (`getIntBE`), PIT change le dernier `i++` de `int b3 = data[i++] & 0xFF;` en `i--`. L'octet lu reste `data[i]`, puisque l'incrément a lieu après la lecture, et `i` n'est plus jamais lu ensuite. Le comportement de la méthode est donc identique : aucun test ne peut tuer ces mutants.

**Une seule ligne n'est pas couverte.** Avec les quatre suites, JaCoCo ne signale plus que la ligne 32 ([rapport](tache2/jacoco-4-avec-tests-de-couverture/org.apache.tika.io/EndianUtils.java.html#L32)) : c'est le constructeur implicite `EndianUtils()`. Toutes les méthodes de la classe sont statiques et rien ne crée d'instance. Un test qui appellerait `new EndianUtils()` ferait monter la couverture sans vérifier aucun comportement : il n'a donc pas été écrit. PIT ne génère d'ailleurs aucun mutant sur cette ligne.

## 7. GitHub Action

Le workflow [`.github/workflows/tache2.yml`](.github/workflows/tache2.yml) s'exécute à chaque push sur `main` qui modifie `tika-core` ou le workflow, et peut aussi être lancé à la main. Il :

1. compile `tika-core` et exécute tous ses tests, dont les nouveaux (`./mvnw -pl tika-core -am install`) ;
2. mesure la couverture JaCoCo d'`EndianUtils` pour chacune des quatre suites de tests ;
3. lance PIT pour chacune des quatre suites, en excluant les tests ajoutés après la mesure (`-DexcludedTestClasses`) ;
4. affiche les résultats dans le résumé de l'exécution et conserve les rapports dans l'artefact `tache2-rapports`, rangé comme le dossier `tache2/`.

ChatUniTest n'y est pas relancé : il lui faut un LLM local, et la génération prend plusieurs heures.

L'exécution [n° 37855166757](https://github.com/Toky5/tika/actions/runs/37855166757) a réussi : les 782 tests de `tika-core`, dont les 37 qui portent sur `EndianUtils`, passent sans échec (2 tests de Tika sont ignorés). Toutes les exécutions : <https://github.com/Toky5/tika/actions/workflows/tache2.yml>. Les workflows d'origine d'Apache Tika ont été désactivés sur ce fork : ils construisent tout le projet et publient des images Docker avec des secrets que le fork n'a pas.

## 8. Déclaration d'utilisation de l'IA

L'énoncé autorise l'utilisation de l'IA à condition de la documenter. Voici les usages.

**ChatUniTest avec CodeQwen1.5-7B-Chat**, l'outil étudié par la tâche. Il a généré les 12 fichiers `EndianUtils_*_Test.java`. Les versions brutes, le journal et les échanges avec le modèle sont dans le dépôt (voir « Fichiers »), et les corrections sont détaillées à la section 3.3.

**Claude, Opus 5.5**, l'outil à été utilisé pour aider à rediger le readme (mise en forme et saisie), assister sur les taches difficiles.

## 9. Conclusion : EndianUtils avant et après

| `EndianUtils` | Avant : tests d'origine | Après : tous les tests |
|---|---|---|
| Tests qui portent sur la classe | 4 | 37 (4 d'origine + 22 générés + 11 écrits à la main) |
| Méthodes couvertes | 4 / 32 | 31 / 32 |
| Lignes couvertes | 31 / 121 (25 %) | 120 / 121 (99 %) |
| Branches couvertes | 10 / 28 (35 %) | 28 / 28 (100 %) |
| Instructions couvertes | 158 / 685 (23 %) | 682 / 685 (99 %) |
| Mutants tués / survivants / non couverts | 38 / 14 / 154 | 204 / 2 / 0 |
| Score de mutation | 18 % | 99 % |
| Force des tests | 73 % | 99 % |

Étape par étape :

| Mesure | Tests | Lignes couvertes | Branches couvertes | Mutants tués | Survivants | Non couverts | Score |
|---|---|---|---|---|---|---|---|
| 1. Tests d'origine | 4 | 31 / 121 | 10 / 28 | 38 | 14 | 154 | 18 % |
| 2. + tests générés | 26 | 60 / 121 | 10 / 28 | 86 | 21 | 99 | 42 % |
| 3. + tests pour les survivants | 31 | 61 / 121 | 12 / 28 | 105 | 2 | 99 | 51 % |
| 4. + tests pour les mutants non couverts | 37 | 120 / 121 | 28 / 28 | 204 | 2 | 0 | 99 % |

- **Les tests générés par ChatUniTest ont aidé, mais pas seuls.** Ils couvrent 29 lignes de plus et tuent 48 mutants de plus, mais n'ajoutent aucune branche : le modèle n'a produit de test pour aucune méthode `read*`, où se trouvent presque toutes les conditions. Aucun n'était utilisable sans retouche, et 3 oracles sur 22 étaient faux, alors que ChatUniTest les présentait comme réussis.
- **Les tests écrits à la main ont comblé le reste.** 5 tests ont tué les 19 survivants non équivalents, presque sans changer la couverture : la couverture seule ne dit pas si les vérifications sont fortes. 6 autres ont couvert les 14 méthodes jamais appelées et tué les 99 mutants restants.
- **Il ne reste que l'irréductible** : 2 mutants équivalents, qu'aucun test ne peut tuer, et la ligne du constructeur implicite, qu'aucun comportement ne justifie de tester.
