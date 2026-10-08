# IFT3913 — Tâche 2 : tests générés par un LLM et analyse de mutation de `EndianUtils` (Apache Tika)

Ce dépôt est une copie (fork) d'[Apache Tika](https://github.com/apache/tika). Ce README documente la tâche 2 ; le README d'origine du projet est conservé dans [`README-apache-tika.md`](README-apache-tika.md).

## Sommaire

1. [Résumé](#1-résumé)
2. [Organisation du dépôt et reproduction](#2-organisation-du-dépôt-et-reproduction)
3. [Choix de la classe](#3-choix-de-la-classe)
4. [ChatUniTest dans le pipeline Maven](#4-chatunitest-dans-le-pipeline-maven)
5. [Génération des tests](#5-génération-des-tests)
6. [Intégration des tests générés et corrections](#6-intégration-des-tests-générés-et-corrections)
7. [Oracles : tests générés et tests écrits à la main](#7-oracles--tests-générés-et-tests-écrits-à-la-main)
8. [Analyse de mutation avec PIT](#8-analyse-de-mutation-avec-pit)
9. [Mutants détectés grâce aux tests générés](#9-mutants-détectés-grâce-aux-tests-générés)
10. [Mutants qui survivent aux tests générés](#10-mutants-qui-survivent-aux-tests-générés)
11. [Tests écrits à la main pour les mutants survivants](#11-tests-écrits-à-la-main-pour-les-mutants-survivants)
12. [Tests écrits à la main pour le code non couvert](#12-tests-écrits-à-la-main-pour-le-code-non-couvert)
13. [Mutants encore vivants](#13-mutants-encore-vivants)
14. [Exécution dans GitHub Actions](#14-exécution-dans-github-actions)
15. [Déclaration d'utilisation de l'IA](#15-déclaration-dutilisation-de-lia)

---

## 1. Résumé

La classe étudiée est **`org.apache.tika.io.EndianUtils`**, du module **`tika-core`**. Elle lit des entiers en little-endian (LE), en big-endian (BE) ou en « middle-endian », depuis un flux (méthodes `read*`) ou depuis un tableau d'octets (méthodes `get*`).

Les tests ont été générés par **ChatUniTest 2.1.1**, branché dans le build Maven, avec un LLM ouvert exécuté localement : **CodeQwen1.5-7B-Chat**, servi par **Ollama**. Leur effet a été mesuré avec **PIT 1.30.0** et **JaCoCo**. Des tests ont ensuite été écrits à la main, en deux temps : pour les mutants qui survivaient, puis pour le code qu'aucun test n'exécutait.

| Suite de tests | Tests d'`EndianUtils` | Mutants tués | Survivants | Non couverts | Score de mutation | Force des tests | Lignes couvertes (JaCoCo) |
|---|---|---|---|---|---|---|---|
| 1. Tests d'origine (`EndianUtilsTest`) | 4 | 38 | 14 | 154 | 18 % (38/206) | 73 % | 31 / 121 |
| 2. + tests générés par ChatUniTest | 4 + 22 | 86 | 21 | 99 | 42 % (86/206) | 80 % | 60 / 121 |
| 3. + tests écrits à la main pour les survivants | 4 + 22 + 5 | 105 | 2 | 99 | 51 % (105/206) | 98 % | 61 / 121 |
| 4. + tests écrits à la main pour le code non couvert | 4 + 22 + 5 + 6 | 204 | 2 | 0 | 99 % (204/206) | 99 % | 120 / 121 |

Le *score de mutation* rapporte les mutants tués au total des mutants ; la *force des tests* les rapporte aux seuls mutants exécutés par au moins un test.

Points principaux :

- ChatUniTest a produit un test pour **12 des 31 méthodes**. Il a échoué sur **les 12 méthodes `read*`**, surtout parce que le modèle ne savait ni importer ni déclarer l'exception interne `EndianUtils.BufferUnderrunException` (11 cas sur 12).
- Aucun test généré ne passait tel quel le build de Tika (règles Checkstyle). Après des corrections de forme automatiques, **16 méthodes de test sur 22 passaient** ; les **6 autres** ont demandé une correction d'une ligne chacune, dont 3 oracles faux.
- ChatUniTest a pourtant présenté ces 6 tests en échec comme réussis : il accepte un test dont une assertion échoue.
- Les tests générés tuent **48 mutants de plus**. **5 tests écrits à la main** tuent ensuite les **19 survivants qui pouvaient l'être**, et **6 autres** tuent les **99 mutants qu'aucun test n'exécutait**. Les 2 derniers survivants sont des mutants équivalents.
- Chacun de ces 11 tests est le seul à tuer certains mutants ou à couvrir certaines lignes ou branches : les tests redondants ont été retirés (section 12).
- Avec toutes les suites, JaCoCo couvre **toutes les branches (28/28)** et **120 des 121 lignes** d'`EndianUtils`. La ligne restante est le constructeur implicite d'une classe dont toutes les méthodes sont statiques.

## 2. Organisation du dépôt et reproduction

| Chemin | Contenu |
|---|---|
| [`tika-core/pom.xml`](tika-core/pom.xml) | Configuration de PIT et de ChatUniTest, et dépendances de test (blocs « Tâche 2 ») |
| [`tika-core/chatunitest-tests/tika-parent/tika-core/org/apache/tika/io/`](tika-core/chatunitest-tests/tika-parent/tika-core/org/apache/tika/io/) | Les 12 tests **bruts**, tels que ChatUniTest les a écrits |
| [`tika-core/src/test/java/org/apache/tika/io/EndianUtils_*_Test.java`](tika-core/src/test/java/org/apache/tika/io/) | Les mêmes tests, **intégrés** à la suite de Tika et corrigés (section 6) |
| [`tika-core/src/test/java/org/apache/tika/io/EndianUtilsMutationTest.java`](tika-core/src/test/java/org/apache/tika/io/EndianUtilsMutationTest.java) | Les tests écrits à la main pour les mutants survivants (section 11) |
| [`tika-core/src/test/java/org/apache/tika/io/EndianUtilsCouvertureTest.java`](tika-core/src/test/java/org/apache/tika/io/EndianUtilsCouvertureTest.java) | Les tests écrits à la main pour le code non couvert (section 12) |
| [`tache2/chatunitest-EndianUtils.log`](tache2/chatunitest-EndianUtils.log) | Journal complet de la génération |
| [`tache2/chatunitest-info/`](tache2/chatunitest-info/) | Pour chaque tentative : demande envoyée au modèle, réponse, code extrait et erreurs (`history…/…/records.json`, `error-message/`) |
| [`tache2/pit-1-tests-originaux/`](tache2/pit-1-tests-originaux/), [`pit-2-avec-tests-generes/`](tache2/pit-2-avec-tests-generes/), [`pit-3-avec-tests-manuels/`](tache2/pit-3-avec-tests-manuels/), [`pit-4-avec-tests-de-couverture/`](tache2/pit-4-avec-tests-de-couverture/) | Rapports PIT (HTML et XML) des quatre mesures du tableau ci-dessus |
| [`tache2/jacoco-1-tests-originaux/`](tache2/jacoco-1-tests-originaux/index.html), [`jacoco-2-avec-tests-generes/`](tache2/jacoco-2-avec-tests-generes/index.html), [`jacoco-3-avec-tests-manuels/`](tache2/jacoco-3-avec-tests-manuels/index.html), [`jacoco-4-avec-tests-de-couverture/`](tache2/jacoco-4-avec-tests-de-couverture/index.html) | Rapports JaCoCo (HTML) d'`EndianUtils` des quatre mesures : le code source, avec les lignes couvertes en vert, partiellement couvertes en jaune et non couvertes en rouge |
| [`tache2/couverture-jacoco.csv`](tache2/couverture-jacoco.csv) | Les chiffres de couverture JaCoCo d'`EndianUtils` des quatre mesures |
| [`.github/workflows/tache2.yml`](.github/workflows/tache2.yml) | GitHub Action qui exécute les tests, puis produit les rapports PIT et la couverture JaCoCo des quatre mesures (section 14) |

Sur GitHub, un fichier HTML s'affiche sous forme de code : pour voir les rapports PIT et JaCoCo, il faut cloner le dépôt (ou télécharger son archive) et ouvrir les fichiers `index.html` dans un navigateur.

Commandes, depuis la racine du dépôt (sous Linux ou macOS, remplacer `.\mvnw.cmd` par `./mvnw`) :

```
:: Compiler tika-core et exécuter tous ses tests
.\mvnw.cmd -pl tika-core -am install

:: Analyse de mutation avec tous les tests (mesure 4 ; rapport : tika-core\target\pit-reports\index.html) ;
:: ajouter -DfullMutationMatrix=true pour que mutations.xml liste tous les tests qui tuent chaque mutant
.\mvnw.cmd -pl tika-core org.pitest:pitest-maven:mutationCoverage

:: Mesures 1, 2 et 3 : exclure les tests ajoutés après la mesure voulue
.\mvnw.cmd -pl tika-core org.pitest:pitest-maven:mutationCoverage -DexcludedTestClasses=org.apache.tika.io.EndianUtils_*_Test,org.apache.tika.io.EndianUtilsMutationTest,org.apache.tika.io.EndianUtilsCouvertureTest
.\mvnw.cmd -pl tika-core org.pitest:pitest-maven:mutationCoverage -DexcludedTestClasses=org.apache.tika.io.EndianUtilsMutationTest,org.apache.tika.io.EndianUtilsCouvertureTest
.\mvnw.cmd -pl tika-core org.pitest:pitest-maven:mutationCoverage -DexcludedTestClasses=org.apache.tika.io.EndianUtilsCouvertureTest

:: Couverture JaCoCo de la mesure 1 (rapport : tika-core\target\site\jacoco\index.html) ;
:: pour les mesures suivantes, ajouter à -Dtest EndianUtils_*_Test, puis EndianUtilsMutationTest, puis EndianUtilsCouvertureTest
.\mvnw.cmd -pl tika-core test -Djacoco.append=false -Dtest=EndianUtilsTest

:: Régénérer les tests (Ollama démarré ; environ 4 heures sur un ordinateur portable)
ollama pull codeqwen:v1.5-chat
.\mvnw.cmd -pl tika-core chatunitest:class -DselectClass=EndianUtils
```

Environnement utilisé : Windows, JDK 17 (Temurin 17.0.20.1), Maven 3.9.12 (via le Maven Wrapper du projet), Ollama avec `codeqwen:v1.5-chat`. Les rapports PIT et la couverture JaCoCo du dossier `tache2/` viennent de la GitHub Action (Ubuntu, JDK 17 Temurin, section 14).

## 3. Choix de la classe

L'énoncé demande une classe des modules étudiés, qui a déjà des tests mais dont la couverture n'atteint pas 100 %. `EndianUtils` remplit ces conditions, avec beaucoup de marge :

| Mesure, avec la suite de tests d'origine de `tika-core` | Valeur |
|---|---|
| Couverture JaCoCo des instructions | 23 % |
| Couverture JaCoCo des branches | 35 % |
| Lignes couvertes (JaCoCo) | 31 / 121 |
| Méthodes couvertes (JaCoCo) | 4 / 32 |
| Mutants PIT : tués / survivants / non couverts | 38 / 14 / 154 (sur 206) |

Le rapport JaCoCo de cette mesure, [`tache2/jacoco-1-tests-originaux`](tache2/jacoco-1-tests-originaux/org.apache.tika.io/EndianUtils.java.html), montre en rouge les 90 lignes qu'aucun test d'origine n'exécute, et le rapport PIT correspondant, [`tache2/pit-1-tests-originaux`](tache2/pit-1-tests-originaux/index.html), les mutants vivants.

Pourquoi cette classe :

- **Elle a 31 méthodes publiques et statiques, sans état** : chaque résultat se calcule à la main à partir des octets d'entrée. On peut donc vérifier chaque oracle produit par le LLM.
- **Ses tests d'origine sont minces.** `EndianUtilsTest` contient 4 tests, qui n'appellent que 4 méthodes (`readUE7`, `readUIntLE`, `readUIntBE`, `readIntME`). Les 27 autres ne sont jamais exécutées.
- **Les tests d'origine ont un trou.** Dans `testReadUIntBE`, le cas du flux trop court appelle `readUIntLE` au lieu de `readUIntBE` ([`EndianUtilsTest.java`, ligne 68](tika-core/src/test/java/org/apache/tika/io/EndianUtilsTest.java#L68)). La détection de fin de flux de `readUIntBE` n'est donc jamais vérifiée : 4 mutants survivent à la ligne 111.
- **Elle manipule des octets** (masques `& 0xFF`, décalages, extension de signe), une source classique d'erreurs, ce qui produit beaucoup de mutants arithmétiques.

## 4. ChatUniTest dans le pipeline Maven

### 4.1 Le modèle

Le modèle est **CodeQwen1.5-7B-Chat** (`codeqwen:v1.5-chat`, 4,2 Go), servi par **Ollama** sur l'ordinateur de l'étudiant, par son API compatible OpenAI (`http://localhost:11434/v1/chat/completions`). D'après `ollama ps`, il tournait à 52 % sur la carte graphique et à 48 % sur le processeur, faute de mémoire graphique suffisante. Sur une courte demande, il produisait 11,9 jetons par seconde.

La taille de contexte d'Ollama a été fixée à 8 192 jetons (`setx OLLAMA_CONTEXT_LENGTH 8192`). En pratique, l'échange le plus long de la génération faisait 2 876 jetons (demande et réponse) : même la valeur par défaut de 4 096 n'aurait rien tronqué.

### 4.2 Le plugin

Le plugin est déclaré dans [`tika-core/pom.xml`](tika-core/pom.xml) :

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

### 4.3 Les dépendances de test

Pour compiler et exécuter les tests qu'il produit, ChatUniTest demande `chatunitest-starter`. Ce paquet apporte de vieilles versions de Mockito (3.8), de ByteBuddy (1.10), le moteur JUnit Vintage et `junit-platform-runner`. Elles sont incompatibles avec Java 17 et font échouer la règle `dependencyConvergence` de Maven Enforcer, qui interdit d'avoir deux versions d'une même bibliothèque. On les exclut, et on utilise à la place les versions que gère Tika (JUnit par `junit-bom`, Mockito 5) :

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

### 4.4 Lancement

Un premier essai sur une seule méthode a validé l'installation : `.\mvnw.cmd -pl tika-core chatunitest:method -DselectMethod=EndianUtils#ubyteToInt`. Il a pris 3 min 25 s, et le test produit est passé au premier tour. La classe entière a ensuite été traitée avec `chatunitest:class -DselectClass=EndianUtils`.

## 5. Génération des tests

### 5.1 Déroulement

| | |
|---|---|
| Date et durée | 7 octobre 2026, de 18 h 29 à 22 h 33 (4 h 03) |
| Méthodes traitées | 31 (toutes les méthodes publiques, surcharges comprises) |
| Appels au modèle | 72, soit environ 3,4 minutes par appel |
| Délais dépassés | 1 : ChatUniTest attend 5 minutes au plus, puis renvoie la même demande, qui a abouti |
| Taille des demandes, selon le compteur de ChatUniTest | 790 jetons en moyenne, 1 852 au plus |
| Taille des réponses | 598 jetons en moyenne ; 10 réponses ont atteint la limite de 1 024 et ont été coupées |

ChatUniTest envoie au modèle une demande qui contient la méthode à tester et le contexte de sa classe, extrait le code Java de la réponse, le compile, puis l'exécute. En cas d'erreur, il renvoie le message du compilateur ou de l'exécution au modèle et lui demande de corriger, jusqu'à 3 tours au total.

### 5.2 Résultat par méthode

| Résultat | Méthodes | Nombre |
|---|---|---|
| Test produit au premier tour | `getShortLE(byte[], int)`, `getUShortLE` ×2, `getShortBE(byte[], int)`, `getUShortBE` ×2, `getUIntLE(byte[], int)`, `getUIntBE(byte[], int)`, `ubyteToInt`, `getUByte` | 10 |
| Test produit après réparation | `getIntBE(byte[])` au 2e tour, `getShortBE(byte[])` au 3e tour | 2 |
| Aucun test après 3 tours | les 12 méthodes `read*`, `getShortLE(byte[])`, `getIntLE` ×2, `getIntBE(byte[], int)`, `getUIntLE(byte[])`, `getUIntBE(byte[])`, `getLongLE` | 19 |

La réparation automatique a donc peu servi : sur 21 méthodes ratées au premier tour, 2 seulement ont été rattrapées.

### 5.3 Pourquoi 19 méthodes n'ont aucun test

Erreur du dernier tour, d'après les fichiers de [`tache2/chatunitest-info/error-message/`](tache2/chatunitest-info/error-message/) :

| Cause | Méthodes | Nombre |
|---|---|---|
| `BufferUnderrunException` introuvable : le modèle l'importe depuis `org.apache.tika.exception` ou `org.apache.tika.io`, alors que c'est une classe interne, `EndianUtils.BufferUnderrunException` | `readShortLE`, `readShortBE`, `readUShortLE`, `readUShortBE`, `readUIntLE`, `readUIntBE`, `readIntLE`, `readIntBE`, `readIntME`, `readLongBE` | 10 |
| Même exception, vérifiée mais ni attrapée ni déclarée (`throws`) | `readLongLE` | 1 |
| Valeur supérieure à 127 dans un `byte[]` sans conversion : `new byte[] {0xFF}` ne compile pas, car un octet Java est signé | `readUE7`, `getIntLE(byte[], int)`, `getIntBE(byte[], int)`, `getUIntLE(byte[])`, `getUIntBE(byte[])`, `getLongLE` | 6 |
| Variable non finale utilisée dans une lambda | `getShortLE(byte[])` | 1 |
| Le test plante à l'exécution (`ArrayIndexOutOfBoundsException` : tableau trop court) | `getIntLE(byte[])` | 1 |

Pour `getUIntBE(byte[])`, les deux tours de réparation n'ont même pas fourni de code exploitable. Ailleurs, le modèle recevait bien le message du compilateur, mais ne savait pas en tirer la correction : pour `BufferUnderrunException`, il a essayé d'autres paquets sans jamais trouver la classe interne.

### 5.4 Le modèle ne donne pas toujours le même test

L'essai de la section 4.4 avait produit pour `ubyteToInt` un test juste. Avec le même modèle et les mêmes réglages, la génération complète a produit un test dont l'oracle est faux (section 6.4).

## 6. Intégration des tests générés et corrections

### 6.1 Où sont les tests générés

- **Version brute**, telle que ChatUniTest l'a écrite : [`tika-core/chatunitest-tests/tika-parent/tika-core/org/apache/tika/io/`](tika-core/chatunitest-tests/tika-parent/tika-core/org/apache/tika/io/). Elle est conservée pour comparaison, et le build ne la compile pas.
- **Version intégrée** à la suite de Tika : `tika-core/src/test/java/org/apache/tika/io/EndianUtils_<méthode>_<numéro>_0_Test.java`, 12 classes et 22 méthodes de test. Le numéro est l'indice de la méthode testée dans `EndianUtils`, ce qui distingue les surcharges.

### 6.2 Compilent-ils et s'exécutent-ils sans intervention ?

**Dans ChatUniTest, oui en apparence.** Les 12 tests compilent, et ChatUniTest affiche « compile and execute successfully » pour chacun. Pourtant, sa propre exécution JUnit montre 6 méthodes de test en échec, dans 5 fichiers. ChatUniTest ne rejette un test que s'il ne compile pas ou s'il lève une exception inattendue. Une assertion qui échoue, c'est-à-dire un oracle faux, est acceptée.

**Dans le build de Tika, non.** Checkstyle s'exécute avant la compilation (phase `validate`) et fait échouer le build. Chaque fichier viole au moins trois de ses règles :

- l'en-tête de licence Apache est absent (`RegexpHeader`) ;
- il contient des `import ...*` (`AvoidStarImport`) ;
- il a des imports inutiles (`UnusedImports`) : Mockito, `ExtendWith`, `TikaException`, `IOException` et `InputStream`, que ChatUniTest ajoute à tous les tests.

### 6.3 Corrections de forme, automatiques, sur les 12 fichiers

1. Remplacer les imports `*` par des imports précis, en une commande :

   ```
   powershell -NoProfile -Command "Get-ChildItem tika-core\src\test\java\org\apache\tika\io\EndianUtils_*_Test.java | ForEach-Object { $t = [IO.File]::ReadAllText($_.FullName); $t = $t.Replace('import static org.mockito.Mockito.*;', '').Replace('import org.mockito.*;', '').Replace('import org.junit.jupiter.api.*;', 'import org.junit.jupiter.api.Test;').Replace('import static org.junit.jupiter.api.Assertions.*;', 'import static org.junit.jupiter.api.Assertions.assertEquals;' + [char]10 + 'import static org.junit.jupiter.api.Assertions.assertThrows;'); [IO.File]::WriteAllText($_.FullName, $t) }"
   ```

2. Lancer l'outil de formatage de Tika, `.\mvnw.cmd -pl tika-core spotless:apply`. Il ajoute l'en-tête de licence, supprime les imports inutiles, trie les imports et passe les fins de ligne au format Unix.

Après ces deux étapes, Checkstyle ne signale plus aucune violation, et les 22 méthodes de test s'exécutent : 16 passent et 6 échouent, les mêmes que dans ChatUniTest.

### 6.4 Corrections de fond, à la main : 6 lignes dans 5 fichiers

Chaque valeur a été vérifiée en exécutant `EndianUtils`.

| Test | Problème | Correction | Justification |
|---|---|---|---|
| `EndianUtils_getUShortBE_19_0_Test.testGetUShortBE` | Oracle faux : attend `0x0201` | `0x0203` | Les octets 2 et 3 de `{0, 1, 2, …, 7}` valent `0x02` et `0x03`. |
| `EndianUtils_getUIntLE_25_0_Test.testGetUIntLE` | Oracle faux : lit `{0, 0, 0, 1}` comme du big-endian et attend `1` | `0x01000000L` | En little-endian, le dernier octet est celui de poids fort. |
| `EndianUtils_ubyteToInt_29_0_Test.testUbyteToInt` | Oracle faux : attend `0xF2` pour `(byte) -34` | `0xDE` | -34 sans signe vaut 256 − 34 = 222 = `0xDE`. Le modèle a confondu avec -14, qui vaut `0xF2`. |
| `EndianUtils_getShortBE_16_0_Test.testGetShortBE` | Test contradictoire : après avoir vérifié le résultat, il exige une exception sur le même tableau valide | `getShortBE(new byte[] { 0 })` | Le commentaire du test annonce « un tableau invalide » : avec un seul octet, la lecture déborde bien. |
| `EndianUtils_getShortBE_17_0_Test.testGetShortBEWithNullData` | Appel par réflexion : la `NullPointerException` attendue arrive enveloppée dans une `InvocationTargetException` | Appel direct `EndianUtils.getShortBE(data, offset)` | La méthode est publique : la réflexion était inutile. |
| `EndianUtils_getShortBE_17_0_Test.testGetShortBEWithOffsetOutOfBounds` | Même problème avec `ArrayIndexOutOfBoundsException` | Appel direct | Même raison. |

Après ces corrections, les 22 méthodes de test passent, et la suite complète de `tika-core` aussi : 777 tests, 0 échec, 3 ignorés.

En résumé, **aucun test n'a été intégrable sans intervention**. Les 12 fichiers ont demandé les mêmes corrections de forme, automatisables. 6 méthodes de test sur 22 ont demandé une correction de fond : 3 oracles faux, 1 donnée d'entrée contradictoire et 2 appels par réflexion.

## 7. Oracles : tests générés et tests écrits à la main

Dans cette section, les tests écrits à la main sont ceux d'origine, `EndianUtilsTest`, écrits par les développeurs de Tika. Les tests que nous avons écrits sont décrits aux sections 11 et 12.

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
- **Les données choisies par le modèle détectent moins bien les erreurs arithmétiques.** Avec des octets nuls, un décalage ou une soustraction faussés ne changent pas le résultat : 5 mutants de `getIntLE` survivent pour cette raison (section 10).
- **Les deux suites se complètent.** Les tests écrits à la main couvrent les méthodes `read*`, où le LLM a échoué. Les tests générés couvrent les méthodes `get*`, que personne ne testait.

## 8. Analyse de mutation avec PIT

### 8.1 Configuration

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

### 8.2 Résultats

| Mesure | Rapport | Tués | Survivants | Non couverts | Score | Force des tests |
|---|---|---|---|---|---|---|
| 1. Tests d'origine | [`pit-1-tests-originaux`](tache2/pit-1-tests-originaux/index.html) | 38 | 14 | 154 | 18 % | 73 % |
| 2. + tests générés | [`pit-2-avec-tests-generes`](tache2/pit-2-avec-tests-generes/index.html) | 86 | 21 | 99 | 42 % | 80 % |
| 3. + tests écrits à la main pour les survivants | [`pit-3-avec-tests-manuels`](tache2/pit-3-avec-tests-manuels/index.html) | 105 | 2 | 99 | 51 % | 98 % |
| 4. + tests écrits à la main pour le code non couvert | [`pit-4-avec-tests-de-couverture`](tache2/pit-4-avec-tests-de-couverture/index.html) | 204 | 2 | 0 | 99 % | 99 % |

Entre les mesures 1 et 2, 48 mutants non couverts deviennent tués et 7 deviennent survivants. Entre les mesures 2 et 3, 19 survivants deviennent tués. Entre les mesures 3 et 4, les 99 mutants non couverts deviennent tués.

Ces chiffres et les rapports de `tache2/` viennent de la GitHub Action (section 14), où Maven compile avec javac. Le compilateur compte, car PIT modifie le bytecode et non le code source. Les premières mesures avaient été faites sur des classes compilées par l'extension Java de VS Code, qui utilise le compilateur d'Eclipse : PIT y trouvait 207 mutants. Le mutant en plus était `j--` changé en `j++` dans la boucle de `getLongLE`. Sur le bytecode de javac, PIT reconnaît la boucle `for` et ne crée pas ce mutant, qui pourrait la rendre infinie.

### 8.3 Couverture JaCoCo

JaCoCo, déjà configuré dans Tika, mesure la couverture des mêmes suites de tests (commande à la section 2). Résultats pour `EndianUtils`, d'après [`tache2/couverture-jacoco.csv`](tache2/couverture-jacoco.csv). Chaque rapport HTML montre le code source d'`EndianUtils`, avec les lignes couvertes en vert, partiellement couvertes en jaune (une partie des branches seulement) et non couvertes en rouge. La ligne « Total » d'un rapport HTML compte aussi la classe interne `BufferUnderrunException` (4 instructions, toujours couvertes) ; le tableau ci-dessous ne compte que `EndianUtils`.

| Mesure | Rapport | Instructions | Branches | Lignes | Méthodes |
|---|---|---|---|---|---|
| 1. Tests d'origine | [`jacoco-1-tests-originaux`](tache2/jacoco-1-tests-originaux/index.html) | 158 / 685 (23 %) | 10 / 28 (35 %) | 31 / 121 | 4 / 32 |
| 2. + tests générés | [`jacoco-2-avec-tests-generes`](tache2/jacoco-2-avec-tests-generes/index.html) | 337 / 685 (49 %) | 10 / 28 (35 %) | 60 / 121 | 17 / 32 |
| 3. + tests écrits à la main pour les survivants | [`jacoco-3-avec-tests-manuels`](tache2/jacoco-3-avec-tests-manuels/index.html) | 341 / 685 (49 %) | 12 / 28 (42 %) | 61 / 121 | 17 / 32 |
| 4. + tests écrits à la main pour le code non couvert | [`jacoco-4-avec-tests-de-couverture`](tache2/jacoco-4-avec-tests-de-couverture/index.html) | 682 / 685 (99 %) | 28 / 28 (100 %) | 120 / 121 | 31 / 32 |

Deux remarques :

- **Les tests générés n'ajoutent aucune branche.** Les méthodes qu'ils testent (`get*`, `ubyteToInt`, `getUByte`) ne contiennent aucune condition : toutes les branches d'`EndianUtils` sont dans les méthodes `read*` et dans la boucle de `getLongLE`.
- **La couverture ne dit pas si les vérifications sont fortes.** La mesure 3 n'ajoute qu'une ligne et deux branches, mais tue 19 mutants de plus : ses tests passent surtout par du code déjà exécuté, avec des données et des oracles plus exigeants.

## 9. Mutants détectés grâce aux tests générés

Les 48 mutants que les tests générés tuent en plus étaient tous **non couverts** auparavant : aucun test d'origine n'appelait ces méthodes. La colonne « Tués par » donne tous les tests générés qui tuent ces mutants, d'après la matrice complète de PIT (section 12).

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

14 de ces 48 mutants ne sont tués que par des tests corrigés à la main (section 6.4) : `getUIntLE_25` (11), `ubyteToInt_29` (2) et `getShortBE_16` (1). Sans correction, ces tests échouaient sur le code d'origine, et PIT ne pouvait pas s'en servir.

## 10. Mutants qui survivent aux tests générés

Après l'ajout des tests générés, 21 mutants survivent : les 14 des tests d'origine, que les tests générés ne touchent pas puisqu'ils n'appellent aucune méthode `read*`, et 7 nouveaux.

| Méthode (ligne) | Mutants | Pourquoi ils survivent |
|---|---|---|
| `readUIntLE` (92) | `< 0` changé en `<= 0` ; 2 des 3 OU changés en ET | Aucun test ne lit quatre octets nuls. La fin de flux n'est testée qu'au 4e octet, et un -1 en 4e position suffit à rendre le résultat négatif, même si un OU devient un ET. |
| `readUIntBE` (111) | `< 0` changé en `<= 0` ; les 3 OU changés en ET | Mêmes raisons ; de plus, le cas du flux trop court n'est jamais exécuté, à cause du copier-coller de `testReadUIntBE`. |
| `readIntME` (168) | `< 0` changé en `<= 0` ; 2 OU changés en ET | Comme `readUIntLE`. |
| `readUE7` (235) | `>= 0` changé en `> 0` ; `read++ < max` changé en `<=` ; `read++` changé en `read--` | Aucun test ne contient d'octet `0x00`, ni de valeur codée sur plus de 6 octets. |
| `readUE7` (246) | `i < 0` changé en `i <= 0` | Aucun test ne se termine par un octet `0x00`. |
| `getIntLE` (363) | 2 décalages, 3 additions | Seul le test de `getUIntLE` appelle cette méthode, avec `{0, 0, 0, 1}` : décaler ou soustraire un octet nul ne change rien. |
| `getIntLE` (362), `getIntBE` (388) | Dernier `i++` changé en `i--` | Mutants équivalents (section 13). |

## 11. Tests écrits à la main pour les mutants survivants

Les 5 tests sont dans [`EndianUtilsMutationTest.java`](tika-core/src/test/java/org/apache/tika/io/EndianUtilsMutationTest.java). Deux méthodes utilitaires construisent les données : `flux(...)` donne un flux qui contient exactement les octets indiqués ; `fluxScripte(...)` donne un flux qui renvoie les valeurs indiquées telles quelles, -1 compris, puis -1.

### `quatreOctetsNulsNeSontPasUneFinDeFlux`
- **Intention** : un octet de valeur 0 ne doit pas être pris pour une fin de flux.
- **Données** : quatre octets nuls, lus par `readUIntLE`, `readUIntBE` et `readIntME`. Le OU des quatre octets vaut alors exactement 0, la limite entre une lecture valide (≥ 0) et une fin de flux (< 0).
- **Oracle** : la valeur 0, sans exception.
- **Mutants tués** : `< 0` changé en `<= 0` aux lignes 92, 111 et 168 (3).

### `uneFinDeFluxSuivieDOctetsEstDetectee`
- **Intention** : chacune des quatre lectures doit être vérifiée, et pas seulement la dernière. Pour `readUIntBE`, c'est aussi le cas du flux trop court que le test d'origine voulait vérifier, mais qu'il appliquait par erreur à `readUIntLE` (section 3).
- **Données** : un flux qui renvoie -1, puis 1, 2 et 3. Seule la première lecture échoue, comme sur une console après Ctrl-Z, dont la fin n'est pas définitive. Avec un flux ordinaire, toutes les lectures suivantes renverraient aussi -1, et la dernière suffirait à révéler la fin du flux.
- **Oracle** : `BufferUnderrunException` pour `readUIntLE`, `readUIntBE` et `readIntME`, l'exception que ces méthodes documentent pour un flux trop court.
- **Mutants tués** : les deux premiers OU changés en ET aux lignes 92 et 168, et les trois OU de la ligne 111 (7). Avec un ET, le -1 est combiné à un octet valide : `-1 & 1` vaut 1, et le résultat n'est plus négatif. Le troisième OU des lignes 92 et 168 était déjà tué par les tests d'origine.

### `readUE7AccepteUnDernierGroupeNul`
- **Intention** : un dernier groupe de valeur 0 est valide.
- **Données** : `0x81 0x00`. `0x81` est un groupe de valeur 1 avec le bit de continuation ; `0x00` est le dernier groupe, nul, et se trouve à la limite des comparaisons `>= 0` et `< 0`.
- **Oracle** : 1 × 128 + 0 = 128.
- **Mutants tués** : `>= 0` changé en `> 0` (ligne 235) et `i < 0` changé en `i <= 0` (ligne 246) (2).

### `readUE7NeDecodePasPlusDeSixOctets`
- **Intention** : vérifier la limite de six groupes fixée par le code (`max = 6`).
- **Données** : `0x81` suivi de six `0x80`, puis `0x00` : une valeur codée sur plus de six octets.
- **Oracle** : 2³⁵, soit un groupe de valeur 1 suivi de cinq groupes nuls. Le septième octet est lu, mais ignoré. Aucune documentation ne dit ce qui doit arriver au-delà de six octets : ce test fige le comportement actuel.
- **Mutants tués** : `read++ < max` changé en `<=` et `read++` changé en `read--` (ligne 235) (2).

### `getIntLEAvecQuatreOctetsDistinctsNonNuls`
- **Intention** : chaque octet doit compter dans le résultat, avec son propre poids.
- **Données** : `{1, 2, 3, 4}`, quatre octets distincts et non nuls, là où le test généré utilisait `{0, 0, 0, 1}`.
- **Oracle** : `0x04030201` ; en little-endian, le dernier octet est celui de poids fort.
- **Mutants tués** : 2 décalages et 3 additions à la ligne 363 (5).

PIT le confirme : à la mesure 3, les 19 survivants qui n'étaient pas équivalents sont tués.

## 12. Tests écrits à la main pour le code non couvert

Après la mesure 3, **99 mutants n'étaient exécutés par aucun test**. Ils se trouvent dans 14 méthodes que ni les tests d'origine, ni les tests générés, ni ceux de la section 11 n'appelaient. Ce sont surtout les méthodes `read*` où ChatUniTest a échoué (section 5.3) :

| Méthodes | Mutants non couverts |
|---|---|
| `readShortLE`, `readShortBE` | 1 + 1 |
| `readUShortLE`, `readUShortBE` | 6 + 6 |
| `readIntLE`, `readIntBE` | 12 + 12 |
| `readLongLE`, `readLongBE` | 24 + 24 |
| `getLongLE` | 8 |
| `getShortLE(byte[])`, `getUShortLE(byte[])`, `getIntLE(byte[])`, `getUIntLE(byte[])`, `getUIntBE(byte[])` | 5 × 1 |

Les 6 tests de [`EndianUtilsCouvertureTest.java`](tika-core/src/test/java/org/apache/tika/io/EndianUtilsCouvertureTest.java) appellent ces méthodes. Ils reprennent les méthodes utilitaires `flux(...)` et `fluxScripte(...)` de la section 11, et les mêmes principes : des octets distincts et non nuls, pour que chaque opération compte dans le résultat, et des valeurs à la limite des comparaisons.

### `lectureDOctetsDistinctsDepuisUnFlux`
- **Intention** : les 8 méthodes de lecture jamais appelées (`readShortLE`, `readShortBE`, `readUShortLE`, `readUShortBE`, `readIntLE`, `readIntBE`, `readLongLE`, `readLongBE`) placent chaque octet à sa place, selon l'ordre LE ou BE.
- **Données** : les octets 1, 2, 3… Ils sont distincts et non nuls : un décalage dans le mauvais sens, une soustraction à la place d'une addition ou un octet mal placé change forcément le résultat. Avec des octets nuls, comme dans certains tests générés, ces erreurs passent inaperçues (section 7).
- **Oracle** : la valeur se lit directement en hexadécimal. En LE, le premier octet lu est celui de poids faible : `1, 2` donne `0x0201`. En BE, c'est celui de poids fort : `0x0102`. De même, `1, 2, …, 8` donne `0x0807060504030201` en LE et `0x0102030405060708` en BE.
- **Mutants tués** : tous les décalages, toutes les additions et toutes les valeurs de retour de ces 8 méthodes (52).

### `octetsNulsLusSansFinDeFlux`
- **Intention** : un octet de valeur 0 n'est pas une fin de flux. C'est le comportement vérifié par `quatreOctetsNulsNeSontPasUneFinDeFlux` (section 11), ici pour `readUShortLE`, `readUShortBE`, `readIntLE`, `readIntBE`, `readLongLE` et `readLongBE`.
- **Données** : que des octets nuls. Le OU des octets lus vaut alors exactement 0, la limite entre une lecture valide (≥ 0) et une fin de flux (< 0).
- **Oracle** : la valeur 0, sans exception.
- **Mutants tués** : `< 0` changé en `<= 0` aux lignes 64, 73, 130, 149, 191 et 217 (6).

### `finDeFluxDetecteeMemeSiDesOctetsSuivent`
- **Intention** : chacune des lectures doit être vérifiée, et pas seulement la dernière, comme dans `uneFinDeFluxSuivieDOctetsEstDetectee` (section 11), pour les mêmes 6 méthodes.
- **Données** : un flux qui renvoie -1, puis des octets valides (1, 2, 3…). Seule la première lecture échoue.
- **Oracle** : `BufferUnderrunException`, l'exception que ces méthodes déclarent pour un flux qui ne fournit pas assez d'octets.
- **Mutants tués** : chaque OU changé en ET dans les conditions de fin de flux : 1 + 1 (`readUShort*`), 3 + 3 (`readInt*`) et 7 + 7 (`readLong*`), soit 22. Avec un ET, le -1 est combiné à un octet valide : `-1 & 1` vaut 1, et le résultat n'est plus négatif.

La négation de ces 6 conditions (`< 0` changé en `>= 0`) inverse le test : l'exception est levée sur un flux valide, et ne l'est plus sur un flux tronqué. Ces 6 mutants sont tués par chacun des trois tests précédents.

### `readUE7SignaleUnFluxTronque`
- **Intention** : `readUE7` doit signaler un flux qui s'arrête au milieu d'une valeur.
- **Données** : un flux vide, puis un flux qui ne contient que `0x81`. Le bit de poids fort de `0x81` annonce un octet suivant, qui n'arrive jamais.
- **Oracle** : une `IOException`. La méthode déclare cette exception et la lève avec le message « Buffer underun; expected one more byte » : une valeur incomplète ne doit pas être renvoyée comme si elle était valide.
- **Mutants tués** : aucun que les autres tests ne tuent déjà. Les 5 mutants qu'il tue, comme la négation de `i < 0` (ligne 246), sont aussi tués par `EndianUtilsTest.testReadUE7`. Il est gardé pour la couverture : il couvre la ligne 247 et les deux branches de fin de flux de `readUE7`, le seul code que JaCoCo signalait encore comme non couvert après la mesure 3, en dehors des méthodes jamais appelées et du constructeur. Vérification faite à la main : si on supprime le test `if (i < 0)`, ce test échoue, et c'est le seul.

### `surchargesSansDecalageLisentDepuisLeDebut`
- **Intention** : les 5 surcharges sans décalage jamais appelées (`getShortLE(byte[])`, `getUShortLE(byte[])`, `getIntLE(byte[])`, `getUIntLE(byte[])`, `getUIntBE(byte[])`) lisent à partir de l'indice 0.
- **Données** : `{1, 2, 3, 4}`, quatre octets distincts.
- **Oracle** : la même valeur qu'avec un décalage de 0 : `0x0201` sur 2 octets en LE, `0x04030201` sur 4 octets en LE, `0x01020304` en BE.
- **Mutants tués** : la valeur de retour remplacée par 0, dans chacune des 5 surcharges (5). Elles ne font qu'appeler la version avec décalage, déjà testée : c'est leur seul mutant.

### `getLongLEAvecOctetsDistinctsEtDecalage`
- **Intention** : `getLongLE` assemble 8 octets en little-endian, à partir du décalage donné.
- **Données** : huit octets distincts `{1, …, 8}`, d'abord au début du tableau, puis précédés de deux octets `0x7F` qui doivent être ignorés (décalage de 2). Le tableau a exactement la taille nécessaire : une lecture en dehors des 8 octets lève une exception.
- **Oracle** : `0x0807060504030201` dans les deux cas.
- **Mutants tués** : les 8 mutants de `getLongLE` : les bornes de la boucle (`>=` changé en `>`, condition niée, `offset + LONG_SIZE - 1` changé en `offset - LONG_SIZE - 1` ou en `offset + LONG_SIZE + 1`), le décalage `<<=` changé en `>>=`, le masque `0xff &` changé en `0xff |`, `|=` changé en `&=`, et la valeur de retour (8). PIT ne crée pas de mutant `j--` changé en `j++`, qui pourrait rendre la boucle infinie (section 8.2).

PIT confirme que ces 6 tests tuent les 99 mutants qui n'étaient pas couverts (mesure 4).

### Aucun test écrit à la main n'est redondant

Chaque test écrit à la main doit apporter quelque chose que les autres tests de sa suite n'apportent pas : au moins un mutant qu'il est seul à tuer, ou au moins une ligne ou une branche qu'il est seul à couvrir. Pour le vérifier, chaque test a été retiré tour à tour de sa suite (mesure 3 pour les tests de la section 11, mesure 4 pour ceux de cette section), et on a comparé les mutants tués et la couverture JaCoCo avec et sans lui. Les mutants que chaque test est seul à tuer viennent aussi de la matrice complète de PIT (option `fullMutationMatrix`, qui liste tous les tests qui tuent chaque mutant), dans les rapports `mutations.xml` de `tache2/`.

| Test | Suite | Mutants qu'il est seul à tuer | Couverture qu'il est seul à apporter |
|---|---|---|---|
| `quatreOctetsNulsNeSontPasUneFinDeFlux` | mesure 3 | 3 | — |
| `uneFinDeFluxSuivieDOctetsEstDetectee` | mesure 3 | 7 | 1 ligne, 1 branche |
| `readUE7AccepteUnDernierGroupeNul` | mesure 3 | 2 | — |
| `readUE7NeDecodePasPlusDeSixOctets` | mesure 3 | 2 | 1 branche |
| `getIntLEAvecQuatreOctetsDistinctsNonNuls` | mesure 3 | 5 | — |
| `lectureDOctetsDistinctsDepuisUnFlux` | mesure 4 | 52 | 2 lignes, 2 méthodes |
| `octetsNulsLusSansFinDeFlux` | mesure 4 | 6 | — |
| `finDeFluxDetecteeMemeSiDesOctetsSuivent` | mesure 4 | 22 | 6 lignes, 6 branches |
| `readUE7SignaleUnFluxTronque` | mesure 4 | — | 1 ligne, 2 branches |
| `surchargesSansDecalageLisentDepuisLeDebut` | mesure 4 | 5 | 5 lignes, 5 méthodes |
| `getLongLEAvecOctetsDistinctsEtDecalage` | mesure 4 | 8 | 5 lignes, 2 branches, 1 méthode |

Deux tests ne remplissaient aucune des deux conditions et ont été retirés :

- `readUIntBESurUnFluxTropCourtLeveUneException` lisait trois octets avec `readUIntBE`. `uneFinDeFluxSuivieDOctetsEstDetectee` tue le même mutant et couvre la même ligne.
- `signeDesValeursLues` vérifiait le signe des valeurs lues par les méthodes `read*`. Tous les mutants qu'il tuait sont aussi tués par `lectureDOctetsDistinctsDepuisUnFlux`, et il ne couvrait rien de plus.

`getIntLEAvecQuatreOctetsDistinctsNonNuls` est conservé, même si, dans la suite complète, ses 5 mutants sont aussi tués par `surchargesSansDecalageLisentDepuisLeDebut`, qui lit les mêmes octets par `getIntLE(byte[])`. À la mesure 3, il est le seul à les tuer.

## 13. Mutants encore vivants

**2 mutants survivent, et ils sont équivalents.** Aux lignes 362 (`getIntLE`) et 388 (`getIntBE`), PIT change le dernier `i++` de `int b3 = data[i++] & 0xFF;` en `i--`. L'octet lu reste `data[i]`, puisque l'incrément a lieu après la lecture, et `i` n'est plus jamais lu ensuite. Le comportement de la méthode est donc identique : aucun test ne peut tuer ces mutants.

**Une seule ligne n'est pas couverte.** Avec les quatre suites, JaCoCo ne signale plus que la ligne 32 ([rapport](tache2/jacoco-4-avec-tests-de-couverture/org.apache.tika.io/EndianUtils.java.html#L32)) : c'est le constructeur implicite `EndianUtils()`. Toutes les méthodes de la classe sont statiques et rien ne crée d'instance. Un test qui appellerait `new EndianUtils()` ferait monter la couverture sans vérifier aucun comportement : il n'a donc pas été écrit. PIT ne génère d'ailleurs aucun mutant sur cette ligne.

## 14. Exécution dans GitHub Actions

Le workflow [`.github/workflows/tache2.yml`](.github/workflows/tache2.yml) s'exécute à chaque push sur `main` qui modifie `tika-core` ou le workflow lui-même, et peut aussi être lancé à la main. Il :

1. installe le JDK 17, puis compile `tika-core` et exécute tous ses tests (`./mvnw -pl tika-core -am install`) ;
2. mesure la couverture JaCoCo d'`EndianUtils` pour chacune des quatre suites de tests (`-Dtest=…`), l'écrit dans `couverture-jacoco.csv` et en fait un rapport HTML limité à `EndianUtils` avec l'outil en ligne de commande de JaCoCo (le rapport HTML de Maven couvre tout `tika-core`) ;
3. lance PIT quatre fois, avec la matrice complète (`-DfullMutationMatrix=true`), en excluant à chaque fois les tests ajoutés après la mesure (`-DexcludedTestClasses`) ;
4. affiche dans le résumé de l'exécution les tests de `tika-core`, les tableaux de couverture et de mutation, les mutants survivants et, pour chaque test écrit à la main, le nombre de mutants qu'il est seul à tuer ; les mêmes chiffres apparaissent en annotations sur la page de l'exécution ;
5. conserve les rapports dans l'artefact `tache2-rapports`, rangés comme le dossier `tache2/`, et les rapports complets de Surefire et de JaCoCo dans l'artefact `tache2-traces`.

ChatUniTest n'est pas relancé dans GitHub Actions : il lui faut un LLM local, et la génération prend plusieurs heures.

Exécutions : <https://github.com/Toky5/tika/actions/workflows/tache2.yml>.

L'exécution [n° 37855166757](https://github.com/Toky5/tika/actions/runs/37855166757) a réussi en moins de 3 minutes : 782 tests dans `tika-core`, dont 37 pour `EndianUtils`, sans échec (2 tests ignorés). Les rapports PIT et JaCoCo du dossier `tache2/` viennent de son artefact `tache2-rapports`. Ses quatre mesures sont celles des sections 8 et 12 : 38, 86, 105 puis 204 mutants tués sur 206. Pour chaque test écrit à la main, elle donne aussi le même nombre de mutants qu'il est seul à tuer que le tableau de la section 12.

Les workflows d'origine d'Apache Tika ont été désactivés sur ce fork : ils construisent tout le projet et publient des images Docker avec des secrets que le fork n'a pas. Seuls « no split packages » et ce workflow s'exécutent.

## 15. Déclaration d'utilisation de l'IA

L'énoncé autorise l'utilisation de l'IA à condition de la documenter. Voici les usages.

**ChatUniTest avec CodeQwen1.5-7B-Chat**, l'outil étudié par la tâche. Il a généré les 12 fichiers `EndianUtils_*_Test.java`. Les versions brutes, le journal et les échanges avec le modèle sont dans le dépôt (section 2), et les corrections sont détaillées à la section 6.

**Claude, Opus 5.5**, l'outil à été utilisé pour aider à rediger le readme (mise en forme et saisie), assister sur les taches difficiles.