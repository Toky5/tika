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
11. [Tests écrits à la main](#11-tests-écrits-à-la-main)
12. [Mutants encore vivants](#12-mutants-encore-vivants)
13. [Exécution dans GitHub Actions](#13-exécution-dans-github-actions)
14. [Déclaration d'utilisation de l'IA](#14-déclaration-dutilisation-de-lia)

---

## 1. Résumé

La classe étudiée est **`org.apache.tika.io.EndianUtils`**, du module **`tika-core`**. Elle lit des entiers en little-endian (LE), en big-endian (BE) ou en « middle-endian », depuis un flux (méthodes `read*`) ou depuis un tableau d'octets (méthodes `get*`).

Les tests ont été générés par **ChatUniTest 2.1.1**, branché dans le build Maven, avec un LLM ouvert exécuté localement : **CodeQwen1.5-7B-Chat**, servi par **Ollama**. Leur effet a été mesuré avec **PIT 1.30.0**, puis des tests ont été écrits à la main pour les mutants qui survivaient.

| Suite de tests | Tests d'`EndianUtils` | Mutants tués | Survivants | Non couverts | Score de mutation | Force des tests |
|---|---|---|---|---|---|---|
| Tests d'origine (`EndianUtilsTest`) | 4 | 38 | 14 | 155 | 18 % (38/207) | 73 % |
| + tests générés par ChatUniTest | 4 + 22 | 86 | 21 | 100 | 42 % (86/207) | 80 % |
| + tests écrits à la main | 4 + 22 + 6 | 105 | 2 | 100 | 51 % (105/207) | 98 % |

Le *score de mutation* rapporte les mutants tués au total des mutants ; la *force des tests* les rapporte aux seuls mutants exécutés par au moins un test.

Points principaux :

- ChatUniTest a produit un test pour **12 des 31 méthodes**. Il a échoué sur **les 12 méthodes `read*`**, surtout parce que le modèle ne savait ni importer ni déclarer l'exception interne `EndianUtils.BufferUnderrunException` (11 cas sur 12).
- Aucun test généré ne passait tel quel le build de Tika (règles Checkstyle). Après des corrections de forme automatiques, **16 méthodes de test sur 22 passaient** ; les **6 autres** ont demandé une correction d'une ligne chacune, dont 3 oracles faux.
- ChatUniTest a pourtant présenté ces 6 tests en échec comme réussis : il accepte un test dont une assertion échoue.
- Les tests générés tuent **48 mutants de plus** ; les **6 tests écrits à la main** tuent les **19 survivants qui pouvaient l'être**. Les 2 derniers survivants sont des mutants équivalents.

## 2. Organisation du dépôt et reproduction

| Chemin | Contenu |
|---|---|
| [`tika-core/pom.xml`](tika-core/pom.xml) | Configuration de PIT et de ChatUniTest, et dépendances de test (blocs « Tâche 2 ») |
| [`tika-core/chatunitest-tests/tika-parent/tika-core/org/apache/tika/io/`](tika-core/chatunitest-tests/tika-parent/tika-core/org/apache/tika/io/) | Les 12 tests **bruts**, tels que ChatUniTest les a écrits |
| [`tika-core/src/test/java/org/apache/tika/io/EndianUtils_*_Test.java`](tika-core/src/test/java/org/apache/tika/io/) | Les mêmes tests, **intégrés** à la suite de Tika et corrigés (section 6) |
| [`tika-core/src/test/java/org/apache/tika/io/EndianUtilsMutationTest.java`](tika-core/src/test/java/org/apache/tika/io/EndianUtilsMutationTest.java) | Les tests écrits à la main (section 11) |
| [`tache2/chatunitest-EndianUtils.log`](tache2/chatunitest-EndianUtils.log) | Journal complet de la génération |
| [`tache2/chatunitest-info/`](tache2/chatunitest-info/) | Pour chaque tentative : demande envoyée au modèle, réponse, code extrait et erreurs (`history…/…/records.json`, `error-message/`) |
| [`tache2/pit-1-tests-originaux/`](tache2/pit-1-tests-originaux/), [`pit-2-avec-tests-generes/`](tache2/pit-2-avec-tests-generes/), [`pit-3-avec-tests-manuels/`](tache2/pit-3-avec-tests-manuels/) | Rapports PIT (HTML et XML) des trois mesures du tableau ci-dessus |
| [`.github/workflows/tache2.yml`](.github/workflows/tache2.yml) | GitHub Action qui exécute les tests et les trois analyses PIT (section 13) |

Commandes, depuis la racine du dépôt (sous Linux ou macOS, remplacer `.\mvnw.cmd` par `./mvnw`) :

```
:: Compiler tika-core et exécuter tous ses tests (777 tests dans tika-core)
.\mvnw.cmd -pl tika-core -am install

:: Analyse de mutation avec tous les tests (mesure 3 ; rapport : tika-core\target\pit-reports\index.html)
.\mvnw.cmd -pl tika-core org.pitest:pitest-maven:mutationCoverage

:: Mesure 1 (tests d'origine seulement) : exclure les tests ajoutés
.\mvnw.cmd -pl tika-core org.pitest:pitest-maven:mutationCoverage -DexcludedTestClasses=org.apache.tika.io.EndianUtils_*_Test,org.apache.tika.io.EndianUtilsMutationTest

:: Mesure 2 (tests d'origine et tests générés) : exclure les tests écrits à la main
.\mvnw.cmd -pl tika-core org.pitest:pitest-maven:mutationCoverage -DexcludedTestClasses=org.apache.tika.io.EndianUtilsMutationTest

:: Régénérer les tests (Ollama démarré ; environ 4 heures sur un ordinateur portable)
ollama pull codeqwen:v1.5-chat
.\mvnw.cmd -pl tika-core chatunitest:class -DselectClass=EndianUtils
```

Environnement utilisé : Windows, JDK 17 (Temurin 17.0.20.1), Maven 3.9.12 (via le Maven Wrapper du projet), Ollama avec `codeqwen:v1.5-chat`.

## 3. Choix de la classe

L'énoncé demande une classe des modules étudiés, qui a déjà des tests mais dont la couverture n'atteint pas 100 %. `EndianUtils` remplit ces conditions, avec beaucoup de marge :

| Mesure, avec la suite de tests d'origine de `tika-core` | Valeur |
|---|---|
| Couverture JaCoCo des instructions | 23 % |
| Couverture JaCoCo des branches | 35 % |
| Lignes couvertes (JaCoCo) | 31 / 121 |
| Méthodes couvertes (JaCoCo) | 4 / 32 |
| Mutants PIT : tués / survivants / non couverts | 38 / 14 / 155 (sur 207) |

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

Les tests écrits à la main sont ceux d'origine, `EndianUtilsTest`, écrits par les développeurs de Tika.

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

| Mesure | Rapport | Lignes couvertes (PIT) | Tués | Survivants | Non couverts | Score | Force des tests |
|---|---|---|---|---|---|---|---|
| 1. Tests d'origine | [`pit-1-tests-originaux`](tache2/pit-1-tests-originaux/index.html) | 32 / 126 | 38 | 14 | 155 | 18 % | 73 % |
| 2. + tests générés | [`pit-2-avec-tests-generes`](tache2/pit-2-avec-tests-generes/index.html) | 61 / 126 | 86 | 21 | 100 | 42 % | 80 % |
| 3. + tests écrits à la main | [`pit-3-avec-tests-manuels`](tache2/pit-3-avec-tests-manuels/index.html) | 62 / 126 | 105 | 2 | 100 | 51 % | 98 % |

Entre les mesures 1 et 2, 48 mutants non couverts deviennent tués et 7 deviennent survivants. Entre les mesures 2 et 3, 19 survivants deviennent tués.

## 9. Mutants détectés grâce aux tests générés

Les 48 mutants que les tests générés tuent en plus étaient tous **non couverts** auparavant : aucun test d'origine n'appelait ces méthodes. PIT attribue chaque mutant au premier test qui le tue ; d'autres tests peuvent aussi le tuer.

| Méthode | Lignes | Mutants tués | Tué par | Pourquoi |
|---|---|---|---|---|
| `getShortLE(byte[], int)` | 270 | Valeur de retour remplacée par 0 (1) | `getShortLE_13` | Le test attend `0x3412`. |
| `getUShortLE(byte[], int)` | 291–293 | Masques `& 0xFF` changés en `\| 0xFF` (2), `offset + 1` changé en `offset - 1`, décalage `<<` changé en `>>`, addition changée en soustraction, retour 0 (6) | `getShortLE_13`, par délégation | Avec les octets `0x12` et `0x34`, tous deux non nuls, chaque opération compte dans le résultat. |
| `getShortBE(byte[])` | 303 | Retour 0 (1) | `getShortBE_16` | Le test attend 1. |
| `getShortBE(byte[], int)` | 314 | Retour 0 (1) | `getShortBE_17` | Le test attend -1 pour `{0xFF, 0xFF}`. |
| `getUShortBE(byte[])` | 324 | Retour 0 (1) | `getUShortBE_18` | Le test attend `0x1234`. |
| `getUShortBE(byte[], int)` | 335–337 | Mêmes 6 mutants que `getUShortLE` (6) | `getUShortBE_18` | Même raison, avec `{0x12, 0x34}`. |
| `getIntLE(byte[], int)` | 359–363 | `i++` changé en `i--` sur les 3 premières lectures, masques (4), décalage de l'octet de poids fort, retour 0 (9) | `getUIntLE_25` (oracle corrigé) | Décaler la position de lecture ou remplacer un masque change l'octet lu ; `1 << 24` devient 0 avec `>>`. |
| `getIntBE(byte[])` | 373 | Retour 0 (1) | `getIntBE_22` | |
| `getIntBE(byte[], int)` | 385–389 | `i++` (3), masques (4), décalages (3), additions (3), retour 0 (14) | `getUIntBE_27`, `getIntBE_22` | `getIntBE_22` place un 1 à chaque position tour à tour : chaque décalage et chaque addition compte. |
| `getUIntLE(byte[], int)` | 411 | Masque `0xFFFFFFFF` changé en OU, retour 0 (2) | `getUIntLE_25` (oracle corrigé) | Le OU avec `0xFFFFFFFF` donne `0xFFFFFFFF` au lieu de `0x01000000`. |
| `getUIntBE(byte[], int)` | 433 | Mêmes 2 mutants (2) | `getUIntBE_27` | |
| `ubyteToInt(byte)` | 461 | `& 0xFF` changé en `\| 0xFF`, retour 0 (2) | `ubyteToInt_29` (oracle corrigé) | `0x12 \| 0xFF` vaut `0xFF` et non `0x12`. |
| `getUByte(byte[], int)` | 472 | Mêmes 2 mutants (2) | `getUByte_30` | Le test attend 3. |

14 de ces 48 mutants sont tués par des tests dont l'oracle a été corrigé à la main : `getUIntLE_25` (11), `ubyteToInt_29` (2) et `getShortBE_16` (1). Sans correction, ces tests échouaient sur le code d'origine, et PIT ne pouvait pas s'en servir.

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
| `getIntLE` (362), `getIntBE` (388) | Dernier `i++` changé en `i--` | Mutants équivalents (section 12). |

## 11. Tests écrits à la main

Les 6 tests sont dans [`EndianUtilsMutationTest.java`](tika-core/src/test/java/org/apache/tika/io/EndianUtilsMutationTest.java). Deux méthodes utilitaires construisent les données : `flux(...)` donne un flux qui contient exactement les octets indiqués ; `fluxScripte(...)` donne un flux qui renvoie les valeurs indiquées telles quelles, -1 compris, puis -1.

### `quatreOctetsNulsNeSontPasUneFinDeFlux`
- **Intention** : un octet de valeur 0 ne doit pas être pris pour une fin de flux.
- **Données** : quatre octets nuls, lus par `readUIntLE`, `readUIntBE` et `readIntME`. Le OU des quatre octets vaut alors exactement 0, la limite entre une lecture valide (≥ 0) et une fin de flux (< 0).
- **Oracle** : la valeur 0, sans exception.
- **Mutants tués** : `< 0` changé en `<= 0` aux lignes 92, 111 et 168 (3).

### `readUIntBESurUnFluxTropCourtLeveUneException`
- **Intention** : vérifier le cas que le test d'origine voulait couvrir, mais qu'il appliquait par erreur à `readUIntLE`.
- **Données** : trois octets `0xFF` ; il en manque un pour former un entier de 4 octets.
- **Oracle** : `BufferUnderrunException`, l'exception que la méthode documente pour un flux trop court.
- **Mutants tués** : le dernier OU changé en ET à la ligne 111 (1).

### `uneFinDeFluxSuivieDOctetsEstDetectee`
- **Intention** : chacune des quatre lectures doit être vérifiée, et pas seulement la dernière.
- **Données** : un flux qui renvoie -1, puis 1, 2 et 3. Seule la première lecture échoue, comme sur une console après Ctrl-Z, dont la fin n'est pas définitive. Avec un flux ordinaire, toutes les lectures suivantes renverraient aussi -1, et la dernière suffirait à révéler la fin du flux.
- **Oracle** : `BufferUnderrunException` pour `readUIntLE`, `readUIntBE` et `readIntME`.
- **Mutants tués** : les deux premiers OU changés en ET aux lignes 92, 111 et 168 (6).

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

PIT confirme que chaque test tue exactement les mutants visés : 19 au total.

## 12. Mutants encore vivants

**2 mutants survivent, et ils sont équivalents.** Aux lignes 362 (`getIntLE`) et 388 (`getIntBE`), PIT change le dernier `i++` de `int b3 = data[i++] & 0xFF;` en `i--`. L'octet lu reste `data[i]`, puisque l'incrément a lieu après la lecture, et `i` n'est plus jamais lu ensuite. Le comportement de la méthode est donc identique : aucun test ne peut tuer ces mutants.

**100 mutants ne sont exécutés par aucun test.** Ils se trouvent dans des méthodes qu'aucune des trois suites n'appelle :

| Méthodes | Mutants |
|---|---|
| `readShortLE`, `readShortBE` | 1 + 1 |
| `readUShortLE`, `readUShortBE` | 6 + 6 |
| `readIntLE`, `readIntBE` | 12 + 12 |
| `readLongLE`, `readLongBE` | 24 + 24 |
| `getLongLE` | 9 |
| `getShortLE(byte[])`, `getUShortLE(byte[])`, `getIntLE(byte[])`, `getUIntLE(byte[])`, `getUIntBE(byte[])` | 5 × 1 |

Ce sont pour l'essentiel les méthodes `read*` où ChatUniTest a échoué (section 5.3). Les tests écrits à la main visent les mutants survivants, comme le demande l'énoncé. Ces 100 mutants demanderaient des tests du même type, par exemple la lecture d'octets distincts depuis un flux.

## 13. Exécution dans GitHub Actions

Le workflow [`.github/workflows/tache2.yml`](.github/workflows/tache2.yml) s'exécute à chaque push sur `main` qui modifie `tika-core` ou le workflow lui-même, et peut aussi être lancé à la main. Il :

1. installe le JDK 17, puis compile `tika-core` et exécute tous ses tests (`./mvnw -pl tika-core -am install`) ;
2. lance PIT trois fois, en excluant les bons tests à chaque fois (`-DexcludedTestClasses`), pour reproduire les trois mesures de la section 8 ;
3. affiche dans le résumé de l'exécution le résultat des tests ajoutés, la couverture JaCoCo d'`EndianUtils` et le tableau des trois mesures ;
4. conserve les rapports (PIT, Surefire, JaCoCo) dans l'artefact `tache2-rapports`.

ChatUniTest n'est pas relancé dans GitHub Actions : il lui faut un LLM local, et la génération prend plusieurs heures.

Exécutions : <https://github.com/Toky5/tika/actions/workflows/tache2.yml>.

La première exécution, [n° 37821899615](https://github.com/Toky5/tika/actions/runs/37821899615), a réussi en 3 minutes. Son résumé redonne les trois mesures de la section 8 : 38, 86 puis 105 mutants tués sur 207.

Les workflows d'origine d'Apache Tika ont été désactivés sur ce fork : ils construisent tout le projet et publient des images Docker avec des secrets que le fork n'a pas. Seuls « no split packages » et ce workflow s'exécutent.

## 14. Déclaration d'utilisation de l'IA

L'énoncé autorise l'utilisation de l'IA à condition de la documenter. Voici les usages.

**ChatUniTest avec CodeQwen1.5-7B-Chat**, l'outil étudié par la tâche. Il a généré les 12 fichiers `EndianUtils_*_Test.java`. Les versions brutes, le journal et les échanges avec le modèle sont dans le dépôt (section 2), et les corrections sont détaillées à la section 6.

**Claude, Opus 5.5**, l'outil à été utilisé pour aider à rediger le readme (mise en forme et saisie), assister sur les taches difficiles.