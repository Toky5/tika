# IFT3913 - Tâche 2 : ChatUniTest et PIT sur EndianUtils (Apache Tika)

Ce dépôt est un fork d'[Apache Tika](https://github.com/apache/tika). Le README d'origine de Tika a été renommé en [`README-apache-tika.md`](README-apache-tika.md). Ce README-ci documente la tâche 2, en suivant les étapes de l'énoncé.

Plan :

1. [Classe choisie](#1-classe-choisie)
2. [Installation de ChatUniTest](#2-installation-de-chatunitest)
3. [Tests générés](#3-tests-générés)
4. [Comparaison des oracles](#4-comparaison-des-oracles)
5. [Analyse de mutation avec PIT](#5-analyse-de-mutation-avec-pit)
6. [Tests écrits à la main](#6-tests-écrits-à-la-main)
7. [GitHub Action](#7-github-action)
8. [Utilisation de l'IA](#8-utilisation-de-lia)
9. [Conclusion](#9-conclusion)

Où trouver les fichiers :

- [`tika-core/pom.xml`](tika-core/pom.xml) : configuration de PIT et de ChatUniTest (blocs « Tâche 2 »)
- [`tika-core/chatunitest-tests/.../org/apache/tika/io/`](tika-core/chatunitest-tests/tika-parent/tika-core/org/apache/tika/io/) : les tests générés bruts, tels que ChatUniTest les a écrits
- [`tika-core/src/test/java/org/apache/tika/io/`](tika-core/src/test/java/org/apache/tika/io/) : les tests générés corrigés (`EndianUtils_*_Test.java`) et nos tests (`EndianUtilsMutationTest.java`, `EndianUtilsCouvertureTest.java`)
- [`tache2/chatunitest-EndianUtils.log`](tache2/chatunitest-EndianUtils.log) et [`tache2/chatunitest-info/`](tache2/chatunitest-info/) : le journal de la génération et les échanges avec le modèle
- `tache2/jacoco-*` et `tache2/pit-*` : les rapports JaCoCo et PIT
- [`.github/workflows/tache2.yml`](.github/workflows/tache2.yml) : la GitHub Action

On a fait les mesures sur 4 suites de tests, chacune contient la précédente :

1. les tests d'origine : `EndianUtilsTest` (4 tests) ;
2. \+ les tests générés par ChatUniTest (22 tests) ;
3. \+ nos tests pour les mutants survivants (5 tests, `EndianUtilsMutationTest`) ;
4. \+ nos tests pour le code non couvert (6 tests, `EndianUtilsCouvertureTest`).

## 1. Classe choisie

On a pris `org.apache.tika.io.EndianUtils`, dans `tika-core`. Elle lit des entiers en little-endian, en big-endian ou en « middle-endian », soit depuis un flux (`read*`), soit depuis un tableau d'octets (`get*`).

Elle a déjà des tests (`EndianUtilsTest`), mais ils couvrent peu de choses :

| Avec les tests d'origine | |
|---|---|
| Lignes couvertes | 31 / 121 (25 %) |
| Branches couvertes | 10 / 28 (35 %) |
| Méthodes couvertes | 4 / 32 |
| Mutants PIT (tués / survivants / non couverts) | 38 / 14 / 154, sur 206 |

Pourquoi cette classe :

- Les 4 tests d'origine appellent seulement 4 des 31 méthodes publiques (`readUE7`, `readUIntLE`, `readUIntBE`, `readIntME`). Les 27 autres ne sont jamais exécutées (voir le [rapport JaCoCo](tache2/jacoco-1-tests-originaux/org.apache.tika.io/EndianUtils.java.html)).
- Même dans le code testé, 14 mutants survivent. Par exemple, `testReadUIntBE` appelle `readUIntLE` au lieu de `readUIntBE` pour le cas du flux trop court ([ligne 68](tika-core/src/test/java/org/apache/tika/io/EndianUtilsTest.java#L68)). Donc la fin de flux de `readUIntBE` n'est jamais testée, et 4 mutants survivent à la ligne 111.
- Les méthodes sont statiques et sans état, donc on peut calculer à la main le résultat attendu et vérifier les oracles du LLM.
- Il y a beaucoup de manipulations d'octets (masques, décalages, signe), ce qui donne beaucoup de mutants intéressants.

## 2. Installation de ChatUniTest

ChatUniTest 2.1.1 est ajouté comme plugin Maven dans [`tika-core/pom.xml`](tika-core/pom.xml). Le modèle est CodeQwen1.5-7B-Chat (`codeqwen:v1.5-chat`), un modèle ouvert qu'on fait tourner en local avec Ollama. ChatUniTest l'appelle par l'API compatible OpenAI d'Ollama.

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

Quelques remarques sur les options :

- `apiKeys` doit être non vide pour ChatUniTest, mais Ollama ne l'utilise pas.
- `model` : il faut un nom que ChatUniTest accepte (il a une liste fixe de modèles) et qu'Ollama connaît aussi.
- `testNumber=1` et `maxRounds=3` : un test par méthode et au plus 3 essais, sinon c'est trop long sur un portable.
- `thread=false` : avec un LLM local, les requêtes en parallèle s'accumulent et dépassent le délai de 5 minutes de ChatUniTest.
- `merge=false` : la classe « suite » que ChatUniTest crée utilise JUnit 4 (`@RunWith`), alors que Tika est en JUnit 6.
- `testOutput` : les tests générés vont dans `tika-core/chatunitest-tests/`, en dehors des sources de Tika.

Il faut aussi la dépendance `chatunitest-starter`. On a exclu ses vieilles versions de Mockito, ByteBuddy et JUnit, qui ne marchent pas avec Java 17 et que Maven Enforcer refuse :

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

Pour générer les tests :

```
ollama pull codeqwen:v1.5-chat
.\mvnw.cmd -pl tika-core chatunitest:class -DselectClass=EndianUtils
```

## 3. Tests générés

### Résultat de la génération

Pour chaque méthode, ChatUniTest envoie la méthode au modèle, récupère le code, le compile et l'exécute. S'il y a une erreur, il renvoie le message au modèle, jusqu'à 3 fois. Pour les 31 méthodes, ça a pris 4 h 03 (72 appels au modèle).

- 10 méthodes ont eu un test dès le premier essai : `getShortLE(byte[], int)`, les deux `getUShortLE`, `getShortBE(byte[], int)`, les deux `getUShortBE`, `getUIntLE(byte[], int)`, `getUIntBE(byte[], int)`, `ubyteToInt` et `getUByte`.
- 2 méthodes ont eu un test après réparation : `getIntBE(byte[])` (2e essai) et `getShortBE(byte[])` (3e essai).
- 19 méthodes n'ont rien eu après 3 essais : les 12 méthodes `read*`, `getShortLE(byte[])`, les deux `getIntLE`, `getIntBE(byte[], int)`, `getUIntLE(byte[])`, `getUIntBE(byte[])` et `getLongLE`.

La réparation automatique n'a donc presque pas aidé (2 méthodes sur 21). D'après [`tache2/chatunitest-info/error-message/`](tache2/chatunitest-info/error-message/), les erreurs du dernier essai sont :

- 10 méthodes : `BufferUnderrunException` introuvable. Le modèle l'importe depuis `org.apache.tika.exception` ou `org.apache.tika.io`, alors que c'est une classe interne (`EndianUtils.BufferUnderrunException`). Il n'a jamais trouvé la bonne classe, même avec le message du compilateur.
- 1 méthode (`readLongLE`) : la même exception n'est ni attrapée ni déclarée.
- 6 méthodes : `new byte[] {0xFF}` ne compile pas, car un `byte` Java est signé (il faut un cast).
- 1 méthode (`getShortLE(byte[])`) : variable non finale utilisée dans une lambda.
- 1 méthode (`getIntLE(byte[])`) : le test plante (`ArrayIndexOutOfBoundsException`, tableau trop court).

### Où sont les tests générés

- Version brute : [`tika-core/chatunitest-tests/tika-parent/tika-core/org/apache/tika/io/`](tika-core/chatunitest-tests/tika-parent/tika-core/org/apache/tika/io/). On l'a gardée pour comparer ; le build ne la compile pas.
- Version corrigée : `tika-core/src/test/java/org/apache/tika/io/EndianUtils_<méthode>_<numéro>_0_Test.java`, soit 12 classes et 22 méthodes de test. Le numéro est l'indice de la méthode dans `EndianUtils` (pour distinguer les surcharges).

### Est-ce qu'ils compilent et passent sans intervention ?

Non.

Dans ChatUniTest, les 12 tests compilent et il affiche « compile and execute successfully » pour chacun. Mais dans son propre résultat JUnit, 6 tests échouent (dans 5 fichiers). En fait ChatUniTest rejette un test seulement s'il ne compile pas ou s'il lance une exception inattendue : une assertion fausse est acceptée.

Dans le build de Tika, ça ne passe pas du tout : Checkstyle bloque avant la compilation. Chaque fichier a au moins 3 problèmes : pas d'en-tête de licence Apache, des imports avec `*`, et des imports inutiles (Mockito, `TikaException`, etc.) que ChatUniTest met dans tous les tests.

Corrections de forme (automatiques, sur les 12 fichiers) :

1. remplacer les imports `*` par des imports précis :

   ```
   powershell -NoProfile -Command "Get-ChildItem tika-core\src\test\java\org\apache\tika\io\EndianUtils_*_Test.java | ForEach-Object { $t = [IO.File]::ReadAllText($_.FullName); $t = $t.Replace('import static org.mockito.Mockito.*;', '').Replace('import org.mockito.*;', '').Replace('import org.junit.jupiter.api.*;', 'import org.junit.jupiter.api.Test;').Replace('import static org.junit.jupiter.api.Assertions.*;', 'import static org.junit.jupiter.api.Assertions.assertEquals;' + [char]10 + 'import static org.junit.jupiter.api.Assertions.assertThrows;'); [IO.File]::WriteAllText($_.FullName, $t) }"
   ```

2. lancer `.\mvnw.cmd -pl tika-core spotless:apply`, qui ajoute l'en-tête de licence, enlève les imports inutiles et met les fins de ligne au format Unix.

Après ça, Checkstyle ne dit plus rien et les 22 tests s'exécutent : 16 passent et 6 échouent (les mêmes que dans ChatUniTest).

Corrections à la main : 6 lignes dans 5 fichiers. On a vérifié chaque valeur en exécutant `EndianUtils`.

| Test | Problème | Correction |
|---|---|---|
| `getUShortBE_19.testGetUShortBE` | attend `0x0201`, mais les octets 2 et 3 de `{0, 1, 2, ...}` donnent `0x0203` | `0x0203` |
| `getUIntLE_25.testGetUIntLE` | lit `{0, 0, 0, 1}` comme du big-endian et attend 1 ; en little-endian le dernier octet est le plus fort | `0x01000000L` |
| `ubyteToInt_29.testUbyteToInt` | attend `0xF2` pour `(byte) -34`, alors que 256 - 34 = 222 = `0xDE` (le modèle a confondu avec -14) | `0xDE` |
| `getShortBE_16.testGetShortBE` | après avoir vérifié le résultat, il attend une exception sur le même tableau valide | `getShortBE(new byte[] { 0 })`, un tableau trop court comme le dit le commentaire du test |
| `getShortBE_17.testGetShortBEWithNullData` | appel par réflexion : la `NullPointerException` arrive enveloppée dans une `InvocationTargetException` | appel direct, la méthode est publique |
| `getShortBE_17.testGetShortBEWithOffsetOutOfBounds` | même problème avec `ArrayIndexOutOfBoundsException` | appel direct |

Bilan : aucun test n'était utilisable sans intervention. Les 12 fichiers ont eu besoin des corrections de forme, et 6 tests sur 22 ont eu besoin d'une correction d'une ligne (3 oracles faux, 1 donnée contradictoire, 2 problèmes de réflexion). Après ça, les 22 tests passent.

## 4. Comparaison des oracles

On compare avec les tests d'origine (`EndianUtilsTest`), écrits par les développeurs de Tika.

| | Tests d'origine | Tests générés |
|---|---|---|
| Méthodes testées | 4 méthodes `read*` | 11 méthodes (des `get*`, `ubyteToInt` et `getUByte`), plus 2 par délégation, et aucune `read*` |
| Oracles | valeurs exactes, prises du format (ex. un exemple documenté de middle-endian) | valeurs exactes calculées par le modèle, et des exceptions |
| Données | octets de poids fort (`0xF0`, `0xFF`), ce qui teste le côté non signé | souvent des octets nuls (`{0, 0, 0, 1}`) ou petits (`0x12`, `0x34`) |
| Exceptions | `BufferUnderrunException`, celle qui est documentée | `NullPointerException` et `ArrayIndexOutOfBoundsException`, que la doc ne promet pas |
| Erreurs | 1 copier-coller (`testReadUIntBE` appelle `readUIntLE`) | 6 tests sur 22 (3 valeurs fausses, 1 test contradictoire, 2 réflexions) |

Ce qu'on remarque :

- Quand ils sont justes, les oracles générés sont plutôt bons : ce sont des `assertEquals` sur des valeurs exactes. Par exemple `getIntBE_22` met un 1 à chaque position, puis `0xFF` partout, et tue tous les mutants non équivalents de `getIntBE`.
- Par contre 3 oracles sur 22 sont faux, et ChatUniTest les présente comme réussis. Il faut donc tout relire.
- Les octets nuls choisis par le modèle cachent des erreurs : décaler ou soustraire 0 ne change rien. C'est pour ça que 5 mutants de `getIntLE` survivent (section 5).
- Les deux suites se complètent : les tests d'origine couvrent les `read*`, où le LLM a échoué, et les tests générés couvrent les `get*`, que personne ne testait.
- Il y a aussi du code inutile dans les tests générés : réflexion dans 4 classes, imports inutiles partout, et `getUShortLE_14` teste la mauvaise surcharge.

## 5. Analyse de mutation avec PIT

### Configuration

On a ajouté PIT 1.30.0 dans [`tika-core/pom.xml`](tika-core/pom.xml) :

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

- `pitest-junit5-plugin` sert à exécuter les tests JUnit 5/6 de Tika.
- `EndianUtils*` prend aussi la classe interne `BufferUnderrunException`.
- On garde les mutateurs par défaut de PIT.
- `parseSurefireArgLine=false` est obligatoire, sinon PIT plante sur le `@{surefireArgLine}` (agent JaCoCo) de la config Surefire de Tika.

Commandes (depuis la racine, `./mvnw` sous Linux ou macOS) :

```
:: compiler tika-core et lancer tous les tests
.\mvnw.cmd -pl tika-core -am install

:: mesure 1 : tests d'origine seulement
.\mvnw.cmd -pl tika-core org.pitest:pitest-maven:mutationCoverage -DexcludedTestClasses=org.apache.tika.io.EndianUtils_*_Test,org.apache.tika.io.EndianUtilsMutationTest,org.apache.tika.io.EndianUtilsCouvertureTest

:: mesure 2 : + tests générés
.\mvnw.cmd -pl tika-core org.pitest:pitest-maven:mutationCoverage -DexcludedTestClasses=org.apache.tika.io.EndianUtilsMutationTest,org.apache.tika.io.EndianUtilsCouvertureTest

:: mesures 3 et 4
.\mvnw.cmd -pl tika-core org.pitest:pitest-maven:mutationCoverage -DexcludedTestClasses=org.apache.tika.io.EndianUtilsCouvertureTest
.\mvnw.cmd -pl tika-core org.pitest:pitest-maven:mutationCoverage
```

Le rapport sort dans `tika-core\target\pit-reports\index.html`.

### Score avec les tests d'origine, puis avec les tests générés

| Mesure | Tués | Survivants | Non couverts | Score de mutation | Force des tests | Rapport |
|---|---|---|---|---|---|---|
| 1. Tests d'origine | 38 | 14 | 154 | 18 % (38/206) | 73 % | [`pit-1`](tache2/pit-1-tests-originaux/index.html) |
| 2. + tests générés | 86 | 21 | 99 | 42 % (86/206) | 80 % | [`pit-2`](tache2/pit-2-avec-tests-generes/index.html) |

(Le score de mutation compte les mutants tués sur tous les mutants ; la force des tests seulement sur les mutants exécutés par au moins un test.)

Les tests générés tuent 48 mutants de plus, mais ils ne détectent pas tous les mutants : 21 survivent et 99 ne sont exécutés par aucun test.

### Mutants détectés par les tests générés, et pourquoi

Les 48 mutants en plus étaient tous non couverts avant, parce qu'aucun test d'origine n'appelait ces méthodes. Les tests qui les tuent viennent de la matrice complète de PIT (`mutations.xml`).

| Méthode | Lignes | Mutants tués | Tués par | Pourquoi |
|---|---|---|---|---|
| `getShortLE(byte[], int)` | 270 | retour remplacé par 0 (1) | `getShortLE_13` | le test attend `0x3412` |
| `getUShortLE(byte[], int)` | 291-293 | masques `& 0xFF` changés en `\| 0xFF` (2), `offset + 1` en `offset - 1`, `<<` en `>>`, `+` en `-`, retour 0 (6) | `getUShortLE_14`, `getUShortLE_15`, `getShortLE_13` | avec `0x12` et `0x34` (deux octets non nuls), chaque opération change le résultat |
| `getShortBE(byte[])` | 303 | retour 0 (1) | `getShortBE_16` | le test attend 1 |
| `getShortBE(byte[], int)` | 314 | retour 0 (1) | `getShortBE_17`, `getShortBE_16` | `getShortBE_17` attend -1 pour `{0xFF, 0xFF}` |
| `getUShortBE(byte[])` | 324 | retour 0 (1) | `getUShortBE_18` | le test attend `0x1234` |
| `getUShortBE(byte[], int)` | 335-337 | les mêmes 6 mutants que `getUShortLE` (6) | `getUShortBE_18`, `getUShortBE_19`, `getShortBE_17` et `getShortBE_16` (5 d'entre eux) | même raison, ex. `{0x12, 0x34}` |
| `getIntLE(byte[], int)` | 359-363 | `i++` en `i--` sur les 3 premières lectures, masques (4), décalage de l'octet fort, retour 0 (9) | `getUIntLE_25` (oracle corrigé) | décaler la lecture ou changer un masque change l'octet lu ; `1 << 24` devient 0 avec `>>` |
| `getIntBE(byte[])` | 373 | retour 0 (1) | `getIntBE_22` | |
| `getIntBE(byte[], int)` | 385-389 | `i++` (3), masques (4), décalages (3), additions (3), retour 0 (14) | `getIntBE_22` (les 14), `getUIntBE_27` (9) | `getIntBE_22` met un 1 à chaque position, donc chaque décalage et chaque addition compte |
| `getUIntLE(byte[], int)` | 411 | masque `0xFFFFFFFF` changé en OU, retour 0 (2) | `getUIntLE_25` (oracle corrigé) | avec le OU on obtient `0xFFFFFFFF` au lieu de `0x01000000` |
| `getUIntBE(byte[], int)` | 433 | les mêmes 2 mutants (2) | `getUIntBE_27` | |
| `ubyteToInt(byte)` | 461 | `& 0xFF` en `\| 0xFF`, retour 0 (2) | `ubyteToInt_29` (oracle corrigé) | `0x12 \| 0xFF` donne `0xFF` et pas `0x12` |
| `getUByte(byte[], int)` | 472 | les mêmes 2 mutants (2) | `getUByte_30` | le test attend 3 |

À noter : 14 de ces 48 mutants sont tués seulement par des tests qu'on a corrigés à la main (`getUIntLE_25` : 11, `ubyteToInt_29` : 2, `getShortBE_16` : 1). Sans nos corrections, ces tests échouaient et PIT ne pouvait pas les utiliser.

### Mutants que les tests générés ne détectent pas

21 mutants survivent : les 14 des tests d'origine (les tests générés n'appellent aucune méthode `read*`) et 7 nouveaux.

| Méthode (ligne) | Mutants | Pourquoi ils survivent |
|---|---|---|
| `readUIntLE` (92) | `< 0` en `<= 0`, 2 des 3 OU en ET | aucun test ne lit 4 octets nuls, et la fin de flux est testée seulement au 4e octet |
| `readUIntBE` (111) | `< 0` en `<= 0`, les 3 OU en ET | pareil, et en plus le flux trop court n'est jamais testé (le copier-coller de `testReadUIntBE`) |
| `readIntME` (168) | `< 0` en `<= 0`, 2 OU en ET | comme `readUIntLE` |
| `readUE7` (235) | `>= 0` en `> 0`, `read++ < max` en `<=`, `read++` en `read--` | aucun test avec un octet `0x00` ou une valeur sur plus de 6 octets |
| `readUE7` (246) | `i < 0` en `i <= 0` | aucun test ne finit par `0x00` |
| `getIntLE` (363) | 2 décalages, 3 additions | le seul test utilise `{0, 0, 0, 1}`, et décaler ou soustraire un octet nul ne change rien |
| `getIntLE` (362), `getIntBE` (388) | dernier `i++` en `i--` | mutants équivalents (voir section 6) |

Et 99 mutants ne sont exécutés par aucun test. Ils sont dans 14 méthodes que personne n'appelle, surtout les `read*` où ChatUniTest a échoué :

- `readShortLE`, `readShortBE` : 1 + 1
- `readUShortLE`, `readUShortBE` : 6 + 6
- `readIntLE`, `readIntBE` : 12 + 12
- `readLongLE`, `readLongBE` : 24 + 24
- `getLongLE` : 8
- `getShortLE(byte[])`, `getUShortLE(byte[])`, `getIntLE(byte[])`, `getUIntLE(byte[])`, `getUIntBE(byte[])` : 1 chacun

## 6. Tests écrits à la main

On a écrit des tests pour tous les mutants non détectés, sauf les 2 équivalents. On les a faits rapidement à l'aide de l'IA générative (voir section 8), puis on les a vérifiés avec PIT et JaCoCo. Les deux classes utilisent deux petites méthodes : `flux(...)`, un flux avec exactement les octets donnés, et `fluxScripte(...)`, un flux qui renvoie les valeurs données telles quelles (même -1), puis -1.

### Pour les 21 survivants : `EndianUtilsMutationTest`

**`quatreOctetsNulsNeSontPasUneFinDeFlux`**
- Intention : un octet qui vaut 0 ne doit pas être pris pour une fin de flux.
- Données : 4 octets nuls, lus avec `readUIntLE`, `readUIntBE` et `readIntME`. Le OU des 4 octets vaut alors exactement 0, la limite entre une lecture valide (>= 0) et une fin de flux (< 0).
- Oracle : 0, sans exception.
- Mutants tués : `< 0` en `<= 0` aux lignes 92, 111 et 168 (3).

**`uneFinDeFluxSuivieDOctetsEstDetectee`**
- Intention : chaque lecture doit être vérifiée, pas seulement la dernière. Pour `readUIntBE`, c'est aussi le cas que `testReadUIntBE` voulait tester (section 1).
- Données : un flux qui renvoie -1, puis 1, 2 et 3. Seule la première lecture échoue (comme une console après Ctrl-Z). Avec un flux normal, toutes les lectures suivantes renverraient -1 et la dernière suffirait à voir la fin.
- Oracle : `BufferUnderrunException`, l'exception documentée pour un flux trop court.
- Mutants tués : les deux premiers OU en ET aux lignes 92 et 168, et les 3 OU de la ligne 111 (7). Avec un ET, `-1 & 1` vaut 1, donc le résultat n'est plus négatif.

**`readUE7AccepteUnDernierGroupeNul`**
- Intention : un dernier groupe qui vaut 0 est valide.
- Données : `0x81 0x00`. `0x81` = groupe 1 avec le bit de continuation, `0x00` = dernier groupe, nul, pile à la limite des tests `>= 0` et `< 0`.
- Oracle : 1 × 128 + 0 = 128.
- Mutants tués : `>= 0` en `> 0` (ligne 235) et `i < 0` en `i <= 0` (ligne 246) (2).

**`readUE7NeDecodePasPlusDeSixOctets`**
- Intention : tester la limite de 6 groupes du code (`max = 6`).
- Données : `0x81`, six `0x80`, puis `0x00` (plus de 6 octets).
- Oracle : 2^35 (un groupe 1 suivi de 5 groupes nuls), le 7e octet est lu mais ignoré. La doc ne dit rien sur ce cas, donc ce test fixe le comportement actuel.
- Mutants tués : `read++ < max` en `<=` et `read++` en `read--` (ligne 235) (2).

**`getIntLEAvecQuatreOctetsDistinctsNonNuls`**
- Intention : chaque octet doit compter, avec son propre poids.
- Données : `{1, 2, 3, 4}`, 4 octets différents et non nuls (le test généré utilisait `{0, 0, 0, 1}`).
- Oracle : `0x04030201`, car en little-endian le dernier octet est le plus fort.
- Mutants tués : 2 décalages et 3 additions à la ligne 363 (5).

Résultat (mesure 3) : 105 mutants tués, les 19 survivants non équivalents sont tués ([`pit-3`](tache2/pit-3-avec-tests-manuels/index.html)).

### Pour les 99 non couverts : `EndianUtilsCouvertureTest`

Ces 6 tests appellent les 14 méthodes jamais exécutées, avec la même idée : des octets différents et non nuls, et des valeurs à la limite des comparaisons.

**`lectureDOctetsDistinctsDepuisUnFlux`**
- Intention : les 8 méthodes de lecture jamais appelées (`readShortLE/BE`, `readUShortLE/BE`, `readIntLE/BE`, `readLongLE/BE`) mettent chaque octet à la bonne place.
- Données : les octets 1, 2, 3... Comme ils sont tous différents et non nuls, un mauvais décalage, une soustraction ou un octet mal placé change le résultat.
- Oracle : on lit la valeur directement en hexadécimal. En LE le premier octet est le plus faible (`1, 2` donne `0x0201`), en BE c'est le plus fort (`0x0102`). Pareil sur 8 octets : `0x0807060504030201` en LE et `0x0102030405060708` en BE.
- Mutants tués : tous les décalages, additions et valeurs de retour de ces 8 méthodes (52).

**`octetsNulsLusSansFinDeFlux`**
- Intention : même idée que `quatreOctetsNulsNeSontPasUneFinDeFlux`, pour `readUShortLE/BE`, `readIntLE/BE` et `readLongLE/BE`.
- Données : seulement des octets nuls, donc le OU vaut 0, la limite.
- Oracle : 0, sans exception.
- Mutants tués : `< 0` en `<= 0` aux lignes 64, 73, 130, 149, 191 et 217 (6).

**`finDeFluxDetecteeMemeSiDesOctetsSuivent`**
- Intention : même idée que `uneFinDeFluxSuivieDOctetsEstDetectee`, pour les mêmes 6 méthodes.
- Données : -1, puis des octets valides.
- Oracle : `BufferUnderrunException`.
- Mutants tués : chaque OU changé en ET dans les tests de fin de flux : 1 + 1 (`readUShort*`), 3 + 3 (`readInt*`) et 7 + 7 (`readLong*`), donc 22.

(La négation de ces 6 conditions, `< 0` en `>= 0`, est tuée par chacun des trois tests ci-dessus : 6 mutants.)

**`readUE7SignaleUnFluxTronque`**
- Intention : `readUE7` doit signaler un flux qui s'arrête au milieu d'une valeur.
- Données : un flux vide, puis un flux avec seulement `0x81` (son bit fort annonce un octet de plus, qui n'arrive jamais).
- Oracle : `IOException` (« Buffer underun; expected one more byte »).
- Mutants tués : aucun que les autres ne tuent déjà. Ce test sert à la couverture : c'est le seul qui exécute la ligne 247 et les deux branches de fin de flux de `readUE7`.

**`surchargesSansDecalageLisentDepuisLeDebut`**
- Intention : les 5 surcharges sans décalage (`getShortLE(byte[])`, `getUShortLE(byte[])`, `getIntLE(byte[])`, `getUIntLE(byte[])`, `getUIntBE(byte[])`) lisent à partir de l'indice 0.
- Données : `{1, 2, 3, 4}`.
- Oracle : la même valeur qu'avec un décalage de 0 (`0x0201`, `0x04030201`, `0x01020304`).
- Mutants tués : le retour remplacé par 0 dans les 5 surcharges (5). C'est leur seul mutant, puisqu'elles appellent juste la version avec décalage.

**`getLongLEAvecOctetsDistinctsEtDecalage`**
- Intention : `getLongLE` assemble 8 octets en little-endian à partir du décalage donné.
- Données : `{1, ..., 8}` au début du tableau, puis précédés de deux `0x7F` à ignorer (décalage 2). Le tableau a juste la bonne taille, donc lire en dehors lance une exception.
- Oracle : `0x0807060504030201` dans les deux cas.
- Mutants tués : les 8 mutants de `getLongLE` (bornes de la boucle, `<<=` en `>>=`, `0xff &` en `0xff |`, `|=` en `&=`, retour) (8).

Résultat (mesure 4) : 204 mutants tués sur 206, plus aucun non couvert ([`pit-4`](tache2/pit-4-avec-tests-de-couverture/index.html)).

### Ce qui reste

Les 2 survivants sont équivalents. Aux lignes 362 (`getIntLE`) et 388 (`getIntBE`), PIT change le dernier `i++` de `int b3 = data[i++] & 0xFF;` en `i--`. L'octet lu reste `data[i]` (l'incrément se fait après la lecture) et `i` n'est plus utilisé après, donc le comportement est le même. Aucun test ne peut les tuer.

Une seule ligne n'est pas couverte : la ligne 32, le constructeur implicite `EndianUtils()` ([rapport](tache2/jacoco-4-avec-tests-de-couverture/org.apache.tika.io/EndianUtils.java.html#L32)). Toutes les méthodes sont statiques, donc rien ne crée d'instance. Un test `new EndianUtils()` augmenterait la couverture sans rien vérifier, alors on ne l'a pas fait. PIT ne crée d'ailleurs aucun mutant sur cette ligne.

## 7. GitHub Action

Le workflow [`.github/workflows/tache2.yml`](.github/workflows/tache2.yml) se lance à chaque push sur `main` qui touche `tika-core` ou le workflow (on peut aussi le lancer à la main). Il :

1. compile `tika-core` et lance tous ses tests, dont les nouveaux (`./mvnw -pl tika-core -am install`) ;
2. mesure la couverture JaCoCo d'`EndianUtils` pour les 4 suites de tests ;
3. lance PIT pour les 4 suites, en excluant à chaque fois les tests ajoutés après (`-DexcludedTestClasses`) ;
4. affiche les résultats dans le résumé de l'exécution et garde les rapports dans l'artefact `tache2-rapports`.

ChatUniTest n'est pas relancé dans l'Action : il faut un LLM local et la génération prend plusieurs heures.

Dernière exécution : [n° 38071868974](https://github.com/Toky5/tika/actions/runs/38071868974), réussie. Les 782 tests de `tika-core` passent, dont les 37 qui portent sur `EndianUtils` (2 tests de Tika sont ignorés). Toutes les exécutions : <https://github.com/Toky5/tika/actions/workflows/tache2.yml>. On a désactivé les workflows d'origine de Tika sur le fork, parce qu'ils construisent tout le projet et publient des images Docker avec des secrets qu'on n'a pas.

## 8. Utilisation de l'IA

L'énoncé permet d'utiliser l'IA si on le dit. Voici ce qu'on a utilisé :

- ChatUniTest avec CodeQwen1.5-7B-Chat : c'est l'outil étudié. Il a généré les 12 fichiers `EndianUtils_*_Test.java`. Les versions brutes, le journal et les échanges avec le modèle sont dans le dépôt, et les corrections sont dans la section 3.

**Claude, Opus 5.5**, l'outil à été utilisé pour aider à rediger le readme (mise en forme et saisie), assister sur les taches difficiles.

## 9. Conclusion

| `EndianUtils` | Avant (tests d'origine) | Après (tous les tests) |
|---|---|---|
| Tests sur la classe | 4 | 37 (4 + 22 générés + 11 écrits à la main) |
| Méthodes couvertes | 4 / 32 | 31 / 32 |
| Lignes couvertes | 31 / 121 (25 %) | 120 / 121 (99 %) |
| Branches couvertes | 10 / 28 (35 %) | 28 / 28 (100 %) |
| Mutants tués / survivants / non couverts | 38 / 14 / 154 | 204 / 2 / 0 |
| Score de mutation | 18 % | 99 % |

Étape par étape :

| Mesure | Tests | Lignes | Branches | Tués | Survivants | Non couverts | Score |
|---|---|---|---|---|---|---|---|
| 1. Tests d'origine | 4 | 31 / 121 | 10 / 28 | 38 | 14 | 154 | 18 % |
| 2. + tests générés | 26 | 60 / 121 | 10 / 28 | 86 | 21 | 99 | 42 % |
| 3. + tests pour les survivants | 31 | 61 / 121 | 12 / 28 | 105 | 2 | 99 | 51 % |
| 4. + tests pour le code non couvert | 37 | 120 / 121 | 28 / 28 | 204 | 2 | 0 | 99 % |

En résumé, ChatUniTest a aidé : +29 lignes couvertes et +48 mutants tués. Mais il n'a ajouté aucune branche, parce qu'il n'a réussi aucun test pour les méthodes `read*`, où sont presque toutes les conditions. Et aucun de ses tests n'était utilisable sans retouche (3 oracles faux sur 22, présentés comme réussis).

Nos tests ont fait le reste. Les 5 premiers ont tué les 19 survivants sans presque changer la couverture, ce qui montre que la couverture seule ne dit pas si les tests vérifient bien les choses. Les 6 autres ont couvert les 14 méthodes jamais appelées et tué les 99 mutants restants. Il ne reste que 2 mutants équivalents et la ligne du constructeur implicite.