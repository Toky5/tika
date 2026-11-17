# IFT3913 : Tâche 3, Semgrep dans Apache Tika

*Le rapport de la tâche 2 se trouve maintenant dans [`README-tache2.md`](README-tache2.md).*

<p align="center">
  <a href="https://github.com/semgrep/semgrep">
    <img src="https://github.com/semgrep/semgrep/raw/develop/images/semgrep-logo-dark.svg" height="100" alt="Logo de Semgrep"/>
  </a>
</p>
<h3 align="center">« Code scanning at ludicrous speed. »</h3>
<p align="center"><em>Le slogan et le logo du dépôt open source de Semgrep (voir les crédits, section 6)</em></p>

**Équipe :** Toky Erick Rabenantoandro, Luc Brennan

## 1. Les fichiers modifiés et leur fonctionnement

Pour cette tâche, nous avons modifié deux classes Java de Tika, ajouté un fichier de règles Semgrep, modifié le workflow GitHub Actions et écrit une petite action GitHub maison. Voici où se trouve chaque fichier et ce qu'il fait. Nous avons choisi `TikaResource` et `HttpFetcher` parce que ce sont deux portes d'entrée de Tika sur le réseau : l'une reçoit des requêtes HTTP de n'importe quel client, l'autre va chercher des documents sur Internet.

### TikaResource.java

[`tika-server/tika-server-core/src/main/java/org/apache/tika/server/core/resource/TikaResource.java`](tika-server/tika-server-core/src/main/java/org/apache/tika/server/core/resource/TikaResource.java)

C'est la ressource JAX-RS qui répond aux requêtes envoyées à `/tika` sur tika-server. Elle reçoit un document par PUT ou par POST multipart, et lit les en-têtes HTTP pour remplir les métadonnées comme le nom du fichier et le type MIME. Elle lance ensuite l'analyse et renvoie le contenu extrait dans le format demandé par l'URL (texte, HTML, XML, JSON ou Markdown). Comme elle reçoit directement les données du client, c'est un bon endroit pour introduire des failles d'injection.

### HttpFetcher.java

[`tika-pipes/tika-pipes-plugins/tika-pipes-http/src/main/java/org/apache/tika/pipes/fetcher/http/HttpFetcher.java`](tika-pipes/tika-pipes-plugins/tika-pipes-http/src/main/java/org/apache/tika/pipes/fetcher/http/HttpFetcher.java)

Cette classe du module tika-pipes-http récupère un document à partir d'une URL avant que Tika l'analyse. Elle construit une requête GET avec Apache HttpClient et y ajoute des en-têtes, ainsi qu'un jeton JWT d'authentification si c'est configuré. Elle copie ensuite la réponse dans un fichier temporaire et ajoute aux métadonnées les informations de la connexion, comme le code de statut et le type de contenu. Parce qu'elle gère des connexions HTTPS et des secrets, nous l'avons choisie pour les failles de cryptographie et de TLS.

### regles-tache3.yml

[`tache3/semgrep/regles-tache3.yml`](tache3/semgrep/regles-tache3.yml)

Ce fichier contient les sept règles Semgrep que nous avons écrites, une par vulnérabilité. Chaque règle a un identifiant qui commence par `tache3-`, un message qui explique le problème et la catégorie CWE dans ses métadonnées. Les règles décrivent la forme générale du défaut plutôt que notre code exact, donc elles peuvent aussi trouver le même problème ailleurs dans Tika. Le workflow les donne à Semgrep avec l'option `--config tache3/semgrep`.

### tache2.yml

[`.github/workflows/tache2.yml`](.github/workflows/tache2.yml)

C'est le workflow GitHub Actions qui compile et teste tika-core, et nous y avons ajouté un job `semgrep` qui s'exécute avant le build. Ce job installe Semgrep avec pip sur le runner, ce qui permet à n'importe qui de lancer le workflow sans rien installer chez soi. Il produit un rapport complet publié comme artefact, puis relance Semgrep avec `--baseline-commit` et `--error` pour ne garder que les vulnérabilités absentes du commit précédent. Si le commit en ajoute une, le job échoue et le build ne démarre pas, grâce à `needs: semgrep`.

**Choix de conception :** nous avons ajouté le job au workflow existant plutôt que d'en créer un nouveau, parce que l'énoncé demande que le processus de build lui-même échoue ; avec `needs: semgrep`, la compilation et les tests ne démarrent même pas. Le mode baseline est indispensable, car Tika contient déjà du code que Semgrep signale, et sans lui le workflow serait rouge en permanence. Le commit de référence est la base de la pull request pour une pull request, le commit d'avant le push pour un push, et le parent du commit (`HEAD~1`) pour un lancement manuel ou le premier push d'une branche ; le checkout récupère tout l'historique (`fetch-depth: 0`) pour que ce commit soit disponible. L'analyse combine nos règles et le jeu public `p/java`, pour que le build échoue sur toute nouvelle vulnérabilité détectée par Semgrep et pas seulement sur les nôtres, et Semgrep est installé à chaque exécution dans une plage de versions fixée (`>=1.100,<2`) pour éviter qu'une mise à jour majeure casse le workflow. Le rapport complet est publié avant l'étape bloquante, donc il reste disponible même quand celle-ci échoue.

**Validation :** nous avons validé le workflow avec trois commits successifs. Le premier n'ajoute que les règles et le workflow, sans toucher au code de Tika : le job `semgrep` doit passer, puisque les résultats déjà présents dans Tika font partie de la référence. *[À compléter : lien vers l'exécution.]* Le deuxième ajoute les sept vulnérabilités : le job doit échouer en listant les nouveaux résultats, et le build doit être ignoré. *[À compléter : lien vers l'exécution.]* Le troisième ne modifie que de la documentation : le job doit repasser au vert, car les vulnérabilités du commit précédent font maintenant partie de la référence, ce qui montre que seul le nouveau code bloque le build. *[À compléter : lien vers l'exécution.]* Avant de pousser, nous avons aussi lancé nos règles en local sur les deux fichiers modifiés, avec `semgrep scan --config tache3/semgrep` suivi des deux chemins. *[À compléter : nombre de résultats obtenus.]*

### rickroll/action.yml

[`.github/actions/rickroll/action.yml`](.github/actions/rickroll/action.yml)

C'est une petite action GitHub composite que nous avons écrite en nous inspirant de l'action `random-rickroll`. Le workflow principal l'appelle quand un test de tika-core échoue, grâce à la condition `if: failure()`, et un petit workflow [`rickroll.yml`](.github/workflows/rickroll.yml) l'appelle aussi dès que quelqu'un ouvre une issue. Elle s'occupe alors de remonter le moral de la personne qui vient de casser le build, d'une façon que nous vous laissons découvrir à la section 5. Comme c'est une action réutilisable, n'importe quel autre workflow peut l'appeler avec `uses:`.

## 2. Les vulnérabilités ajoutées

Dans cette section, nous montrons le code que nous avons ajouté dans les deux classes Java. Pour chaque ajout, nous donnons le type de vulnérabilité, la raison pour laquelle le code est vulnérable, la règle Semgrep qui la détecte et le résultat obtenu avec Semgrep. Chaque ajout est marqué par un commentaire `TACHE3-V1` à `TACHE3-V7` : il suffit de chercher « TACHE3 » dans le fichier pour le retrouver. La plupart de ces ajouts ne s'activent qu'avec une propriété système, pour ne pas changer le comportement normal de Tika.

### V1 : TikaResource.java

```java
static int countPreviousUploads(MultivaluedMap<String, String> httpHeaders) {
    String user = httpHeaders.getFirst("X-Tika-User");
    if (user == null) {
        return 0;
    }
    try (Connection conn = DriverManager.getConnection(AUDIT_DB_URL);
            Statement stmt = conn.createStatement()) {
        String sql = "SELECT COUNT(*) FROM uploads WHERE user_name = '" + user + "'";
        try (ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    } catch (SQLException e) {
        LOG.debug("audit database unavailable", e);
        return 0;
    }
}
```

**Vulnérabilité :** injection SQL (CWE-89). La valeur de l'en-tête `X-Tika-User` est collée directement dans la requête, donc un client qui envoie `' OR '1'='1` change le sens de la requête.

**Règle :** `tache3-injection-sql`

**Résultat Semgrep :** *[À compléter : ligne signalée et message.]*

### V2 : TikaResource.java

```java
static void dumpForDebugging(TikaInputStream tis, MultivaluedMap<String, String> httpHeaders) throws IOException {
    if (DEBUG_DUMP_DIR == null) {
        return;
    }
    String fileName = httpHeaders.getFirst("File-Name");
    if (fileName == null) {
        return;
    }
    fileName = fileName.replace("../", "");
    File dayDir = new File(DEBUG_DUMP_DIR, LocalDate.now().toString());
    if (!dayDir.isDirectory() && !dayDir.mkdirs()) {
        throw new IOException("Could not create " + dayDir);
    }
    File target = new File(dayDir, fileName);
    Files.copy(tis.getPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
}
```

**Vulnérabilité :** traversée de chemin (CWE-22). Le nom de fichier vient de l'en-tête `File-Name` et le code essaie de le nettoyer en retirant les `../`, mais ce filtre ne passe qu'une fois : `....//` redevient `../` après le remplacement. Un nom comme `....//....//home/tika/.bashrc` permet donc encore d'écrire hors du répertoire prévu.

**Règle :** `tache3-traversee-de-chemin`

**Résultat Semgrep :** *[À compléter : ligne signalée et message.]*

### V3 : TikaResource.java

```java
@GET
@Produces("text/html")
@Path("hello")
public String getHelloPage(@QueryParam("name") String name) {
    String displayName = (name == null || name.isBlank()) ? "visiteur" : name.trim();
    return "<html><body><h1>Bonjour " + displayName + " !</h1><p>" + GREETING + "</p></body></html>";
}
```

**Vulnérabilité :** XSS réfléchi (CWE-79). Le paramètre `name` passe par la variable `displayName`, qui ne fait que le nettoyer des espaces, puis il est renvoyé sans échappement dans une page HTML, donc un lien comme `/tika/hello?name=<script>...</script>` fait exécuter du JavaScript dans le navigateur de la victime.

**Règle :** `tache3-xss-reflechi`

**Résultat Semgrep :** *[À compléter : ligne signalée et message.]*

### V4 : TikaResource.java

```java
static void mergeSerializedParseContext(InputStream in, ParseContext context) throws IOException {
    try (ObjectInputStream ois = new ObjectInputStream(in)) {
        Map<String, Object> entries = (Map<String, Object>) ois.readObject();
        for (Map.Entry<String, Object> entry : entries.entrySet()) {
            context.set((Class) Class.forName(entry.getKey()), entry.getValue());
        }
    } catch (ClassNotFoundException e) {
        throw new IOException("Could not read serialized parse context", e);
    }
}
```

**Vulnérabilité :** désérialisation de données non fiables (CWE-502). La pièce jointe `config` envoyée par le client est lue avec `readObject()` sans filtre de classes, ce qui peut mener à l'exécution de code avec une chaîne de gadgets.

**Règle :** `tache3-deserialisation`

**Résultat Semgrep :** *[À compléter : ligne signalée et message.]*

### V5 : HttpFetcher.java

```java
private static final String DEFAULT_JWT_SECRET = "tika-fetcher-2026-Sup3rS3cr3t!";

} else if (Boolean.getBoolean("tika.http.useDefaultJwtSecret")) {
    jwtGenerator = new JwtGenerator(new JwtSecretCreds(DEFAULT_JWT_SECRET.getBytes(StandardCharsets.UTF_8),
            httpFetcherConfig.getJwtIssuer(), httpFetcherConfig.getJwtSubject(), httpFetcherConfig.getJwtExpiresInSeconds()));
}
```

**Vulnérabilité :** secret codé en dur (CWE-798). La clé qui signe les jetons JWT est écrite en clair dans un dépôt public, donc n'importe qui peut la lire et fabriquer des jetons valides.

**Règle :** `tache3-secret-en-dur`

**Résultat Semgrep :** *[À compléter : ligne signalée et message.]*

### V6 : HttpFetcher.java

```java
private static String fingerprint(String url) {
    try {
        MessageDigest md = MessageDigest.getInstance("MD5");
        return HexFormat.of().formatHex(md.digest(url.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
        return null;
    }
}
```

**Vulnérabilité :** hachage faible (CWE-328). MD5 n'est plus résistant aux collisions, donc deux entrées différentes peuvent donner la même empreinte et l'empreinte ne garantit rien.

**Règle :** `tache3-hachage-faible`

**Résultat Semgrep :** *[À compléter : ligne signalée et message.]*

### V7 : HttpFetcher.java

```java
private static HttpClient buildTrustAllClient() throws TikaConfigException {
    TrustManager[] trustAll = new TrustManager[]{new X509TrustManager() {
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }
    }};
    try {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAll, new SecureRandom());
        return HttpClients.custom()
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build();
    } catch (GeneralSecurityException e) {
        throw new TikaConfigException("Could not build the TLS context", e);
    }
}
```

**Vulnérabilité :** validation TLS désactivée (CWE-295). Le `TrustManager` accepte n'importe quel certificat et le nom d'hôte n'est plus vérifié, donc une attaque de l'homme du milieu peut lire et modifier le trafic HTTPS.

**Règle :** `tache3-tls-sans-verification`

**Résultat Semgrep :** deux résultats sont attendus, un pour le `TrustManager` et un pour `NoopHostnameVerifier`. *[À compléter : lignes signalées et messages.]*

### Comparaison des rapports avant et après

À chaque exécution, le job publie un rapport complet, sans baseline, comme artefact `semgrep-rapport`. Nous avons donc un rapport « avant », produit sur le commit qui n'ajoute que les règles et le workflow, et un rapport « après », produit sur le commit qui ajoute les vulnérabilités, tous deux avec exactement la même configuration. *[À compléter : liens vers les deux artefacts.]* Le rapport avant compte *[à compléter]* résultats, tous déjà présents dans Tika avant notre travail. Le rapport après en compte *[à compléter]* : nos règles en ajoutent huit, une par vulnérabilité sauf V7 qui en donne deux, et `p/java` en ajoute *[à compléter]*.

Comparer les deux rapports ligne par ligne ne suffit pas, car l'ajout de code décale les numéros de ligne et un ancien résultat déplacé paraîtrait nouveau. Le mode baseline de Semgrep identifie plutôt chaque résultat par la règle, le fichier et le code trouvé. Le journal de l'étape bloquante du commit « après » donne donc exactement la liste des vulnérabilités nouvellement introduites, et c'est lui qui fait échouer le build. *[À compléter : extrait du journal.]*

## 3. Les règles YAML et la méthode utilisée

Les règles sont dans [`tache3/semgrep/regles-tache3.yml`](tache3/semgrep/regles-tache3.yml). Trois d'entre elles restent simples, dans l'esprit des exemples de la tâche 1 : la désérialisation, le secret en dur et le hachage faible. Les quatre autres sont plus complexes, parce que le code qu'elles visent l'est aussi : l'injection SQL utilise le mode taint, la traversée de chemin écarte le bon nettoyage, le XSS suit la valeur à travers une variable, et le TLS fait de la correspondance structurelle. Ci-dessous, la partie de chaque règle qui fait la détection, avec une ligne qui dit la méthode.

### tache3-injection-sql (V1), plus complexe

```yaml
mode: taint
pattern-sources:
  - pattern: (MultivaluedMap $H).getFirst(...)
pattern-sanitizers:
  - pattern: Integer.parseInt(...)
pattern-sinks:
  - patterns:
      - pattern: $STMT.executeQuery($SQL)
      - focus-metavariable: $SQL
```

**Méthode :** mode taint. La source est une valeur venue d'un en-tête HTTP, le puits est le texte passé à `executeQuery`, et Semgrep suit la donnée de l'une à l'autre même quand elle passe par une variable comme `sql`. `Integer.parseInt` arrête le suivi, car un nombre ne peut pas contenir d'injection.

### tache3-traversee-de-chemin (V2), plus complexe

```yaml
patterns:
  - pattern-inside: |
      String $NAME = $H.getFirst(...);
      ...
  - pattern-either:
      - pattern: new File($DIR, $NAME)
      - pattern: Paths.get($DIR, ..., $NAME)
  - pattern-not-inside: |
      $NAME = FilenameUtils.getName(...);
      ...
```

**Méthode :** `pattern-inside` et `pattern-not-inside`. La règle signale un chemin construit avec une variable remplie plus tôt par une valeur d'en-tête HTTP, et ne se tait que si la variable passe par `FilenameUtils.getName`. Un nettoyage maison comme `replace("../", "")` ne l'arrête donc pas, ce qui est voulu puisqu'il se contourne.

### tache3-xss-reflechi (V3), plus complexe

```yaml
patterns:
  - pattern-inside: |
      @Produces($MEDIA)
      $RET $M(..., @QueryParam($N) String $P, ...) { ... }
  - metavariable-regex:
      metavariable: $MEDIA
      regex: .*html.*
  - pattern-either:
      - pattern: return <... $P ...>;
      - pattern: |
          $T $X = <... $P ...>;
          ...
          return <... $X ...>;
  - pattern-not: return <... StringEscapeUtils.escapeHtml4(...) ...>;
  - pattern-not-inside: |
      $T $X = <... StringEscapeUtils.escapeHtml4(...) ...>;
      ...
```

**Méthode :** annotations, `metavariable-regex` et suite d'instructions. La règle se place dans une méthode qui reçoit un `@QueryParam`, et `metavariable-regex` ne garde que celles qui produisent du HTML, pour ne pas signaler les nombreux points d'entrée en `text/plain`. Elle trouve le paramètre renvoyé directement ou à travers une variable intermédiaire comme `displayName`, et elle se tait si la valeur est échappée avec `escapeHtml4`.

### tache3-deserialisation (V4)

```yaml
pattern: (ObjectInputStream $OIS).readObject()
```

**Méthode :** un seul motif avec une métavariable typée. Comme `$OIS` est typé `ObjectInputStream`, un flux protégé d'un autre type ne serait pas signalé.

### tache3-secret-en-dur (V5)

```yaml
pattern-regex: '(?i)String\s+\w*(secret|password|token)\w*\s*=\s*"[^"]{12,}"'
```

**Méthode :** une expression régulière. Un secret en dur se reconnaît à son texte, un nom qui évoque un secret suivi d'une longue valeur entre guillemets, donc une regex convient mieux qu'un motif de code.

### tache3-hachage-faible (V6)

```yaml
pattern-either:
  - pattern: MessageDigest.getInstance("MD5")
  - pattern: MessageDigest.getInstance("SHA-1")
```

**Méthode :** deux motifs réunis par `pattern-either`. La règle signale directement l'appel à `getInstance` avec un algorithme cassé.

### tache3-tls-sans-verification (V7), plus complexe

```yaml
pattern-either:
  - patterns:
      - pattern-inside: new X509TrustManager() { ... }
      - pattern: public void checkServerTrusted(...) {}
  - pattern: NoopHostnameVerifier.INSTANCE
```

**Méthode :** correspondance structurelle. La règle entre dans une classe anonyme qui implémente `X509TrustManager` et cherche une méthode `checkServerTrusted` au corps vide, c'est-à-dire qui ne refuse jamais rien. Elle signale aussi `NoopHostnameVerifier`, donc cette vulnérabilité donne deux résultats.

## 4. Les fix proposés

Pour chaque vulnérabilité, nous proposons une correction. Nous ne l'avons pas appliquée dans le code, puisque l'énoncé demande que les vulnérabilités restent présentes, mais elle montre comment régler le problème. Chaque correction ne correspond plus au motif de la règle, donc Semgrep ne signalerait plus rien.

### Fix V1 : requête paramétrée

```java
String sql = "SELECT COUNT(*) FROM uploads WHERE user_name = ?";
try (Connection conn = DriverManager.getConnection(AUDIT_DB_URL);
        PreparedStatement stmt = conn.prepareStatement(sql)) {
    stmt.setString(1, user);
    try (ResultSet rs = stmt.executeQuery()) {
        return rs.next() ? rs.getInt(1) : 0;
    }
}
```

Avec un `PreparedStatement`, la valeur de l'en-tête est envoyée comme une donnée et jamais comme du code SQL, donc `' OR '1'='1` est cherché tel quel comme nom d'utilisateur.

### Fix V2 : garder seulement le nom du fichier

```java
String fileName = FilenameUtils.getName(httpHeaders.getFirst("File-Name"));
if (fileName == null || fileName.isEmpty()) {
    return;
}
File target = new File(dayDir, fileName);
```

On remplace le `replace("../", "")` par `FilenameUtils.getName` (commons-io, déjà utilisé par Tika), qui enlève tous les répertoires du nom au lieu de chercher un motif précis. `....//....//home/tika/.bashrc` devient `.bashrc` et le fichier reste dans le répertoire prévu.

### Fix V3 : échapper le HTML

```java
String displayName = (name == null || name.isBlank()) ? "visiteur" : StringEscapeUtils.escapeHtml4(name.trim());
return "<html><body><h1>Bonjour " + displayName + " !</h1><p>" + GREETING + "</p></body></html>";
```

`escapeHtml4` transforme `<` et `>` en `&lt;` et `&gt;`, donc le navigateur affiche le script comme du texte au lieu de l'exécuter. Il faut ajouter la dépendance commons-text à tika-server-core ; l'autre option est de répondre en `text/plain`.

### Fix V4 : filtrer les classes désérialisées

```java
try (ObjectInputStream ois = new ObjectInputStream(in)) {
    ois.setObjectInputFilter(ObjectInputFilter.Config.createFilter("java.util.HashMap;java.lang.String;!*"));
    Map<String, Object> entries = (Map<String, Object>) ois.readObject();
```

Le filtre refuse toute classe qui n'est pas dans la liste avant même de la construire, ce qui bloque les chaînes de gadgets. La solution la plus sûre reste de supprimer ce format et de n'accepter que la configuration en JSON, comme avant.

### Fix V5 : lire le secret dans l'environnement

```java
String secret = System.getenv("TIKA_HTTP_JWT_SECRET");
if (StringUtils.isBlank(secret)) {
    throw new TikaConfigException("TIKA_HTTP_JWT_SECRET is not set");
}
jwtGenerator = new JwtGenerator(new JwtSecretCreds(secret.getBytes(StandardCharsets.UTF_8),
        httpFetcherConfig.getJwtIssuer(), httpFetcherConfig.getJwtSubject(), httpFetcherConfig.getJwtExpiresInSeconds()));
```

Le secret n'est plus dans le code : il est donné au démarrage par une variable d'environnement, et le fetcher refuse de démarrer s'il manque au lieu de prendre une valeur par défaut.

### Fix V6 : utiliser SHA-256

```java
MessageDigest md = MessageDigest.getInstance("SHA-256");
```

SHA-256 n'a pas de collision connue, donc l'empreinte identifie vraiment l'URL.

### Fix V7 : garder la vérification TLS

```java
SSLContext sslContext = SSLContexts.custom()
        .loadTrustMaterial(new File(System.getProperty("tika.http.truststore")), null)
        .build();
return HttpClients.custom()
        .setSSLContext(sslContext)
        .build();
```

Au lieu de tout accepter, on ajoute seulement le certificat dont on a besoin dans un magasin de confiance, et on garde le vérificateur de nom d'hôte par défaut. Si aucun certificat interne n'est nécessaire, il suffit de supprimer l'option et de garder le client HTTP normal.

## 5. Côté humour

<p align="center"><img src="https://imgs.search.brave.com/a5qZcBdpt87uvrss2IscJMnalBG7sPExUD_IvGy2Ics/rs:fit:860:0:0:0/g:ce/aHR0cHM6Ly9lZmIy/OC53b3JkcHJlc3Mu/Y29tL3dwLWNvbnRl/bnQvdXBsb2Fkcy8y/MDE0LzA0L3NwYWNl/YmFsbHNfbHVkaWNy/b3VzX3NwZWVkLmpw/Zz93PTMwMCZoPTE2/MA" height="250" alt="Mème Ludicrous speed, tiré du film Spaceballs"/></p>

Semgrep promet d'analyser le code à la vitesse *ludicrous*. Notre CI, elle, échoue maintenant à la vitesse ludicrous dès qu'une vulnérabilité se pointe. Mais un build rouge, c'est triste : un X rouge, trois kilomètres de journal Maven et l'envie de tout lâcher. Nous avons donc décidé que la CI de Tika devait faire une promesse à ses développeurs : ne jamais les laisser tomber, ne jamais les laisser déprimer devant un test qui échoue.

Au départ, nous voulions utiliser l'action `random-rickroll` telle quelle. Petit problème : elle ne sait parler qu'aux issues, et quand un test échoue après un push, il n'y a personne à qui parler. Elle abandonne alors avec un triste « No issue found », et en plus elle tourne sur Node 12, que GitHub a mis à la retraite. Nous avons donc écrit notre propre version, qui garde son esprit et son paramètre `percentage`, mais qui sait consoler quelqu'un qui vient de casser un test.

Le workflow l'appelle juste après les tests, seulement s'ils ont échoué :

```yaml
      - name: Rapport de diagnostic si un test echoue
        if: failure()
        uses: ./.github/actions/rickroll
        with:
          percentage: 100
```

L'action commence par tirer un nombre au hasard entre 0 et 99 et le compare à `percentage`. Avec 100, personne n'y échappe, et le journal affiche fièrement « Gottem!! ». Elle ajoute ensuite au résumé de l'exécution un « rapport de diagnostic de l'échec » très détaillé, avec une animation et un lien vers une explication vidéo, puis une annotation d'erreur qui invite poliment à le consulter. Si l'échec arrive dans une pull request, elle publie aussi le rapport en commentaire, pour que toute l'équipe profite du diagnostic.

Comme nous ne voulions obliger personne à casser un test pour profiter du diagnostic, un deuxième workflow, `rickroll.yml`, se déclenche à l'ouverture de n'importe quelle issue sur le dépôt. Il appelle la même action, toujours avec `percentage: 100`, et celle-ci répond aussitôt par un commentaire dans l'issue. C'est d'ailleurs le retour aux sources : l'action `random-rickroll` d'origine était faite pour les issues.

```yaml
on:
  issues:
    types: [opened]

jobs:
  diagnostic:
    runs-on: ubuntu-latest
    permissions:
      issues: write
    steps:
      - uses: actions/checkout@v6
      - name: Rapport de diagnostic
        uses: ./.github/actions/rickroll
        with:
          percentage: 100
```

```bash
if [ $((RANDOM % 100)) -ge "$PERCENTAGE" ]; then
  echo "Fine, you win this time."
  exit 0
fi
echo "Gottem!!"
echo "::error title=Un test a echoue::Un rapport de diagnostic detaille est disponible : $VIDEO"
echo "$RAPPORT" >> "$GITHUB_STEP_SUMMARY"
```

Nous ne dirons pas ce que contient la vidéo. Disons seulement qu'elle est très populaire, qu'elle date de 1987 et que tout développeur devrait la voir au moins une fois. Pour le découvrir, il suffit de casser un test, ou plus simplement d'ouvrir une issue sur notre dépôt. Nous vous le recommandons vivement.

*[À compléter : capture d'écran du commentaire laissé dans une issue de test, et du résumé d'une exécution où nous avons cassé exprès une assertion d'un test de tika-core.]*

## 6. Crédits

Ce rapport et le code qui l'accompagne ont été réalisés avec l'aide de l'assistant d'IA Claude (Anthropic). Nous l'avons utilisé pour écrire les règles Semgrep, le job du workflow, l'action de diagnostic et le code des vulnérabilités, et pour rédiger une première version de ce rapport ; nous avons ensuite relu, simplifié, testé et corrigé l'ensemble nous-mêmes.

Le logo et le slogan « Code scanning at ludicrous speed. » affichés en haut du rapport viennent du README du dépôt open source de Semgrep, [github.com/semgrep/semgrep](https://github.com/semgrep/semgrep), distribué sous licence LGPL 2.1 ; l'image est chargée directement depuis ce dépôt ([`images/semgrep-logo-dark.svg`](https://github.com/semgrep/semgrep/raw/develop/images/semgrep-logo-dark.svg)). Le slogan lui-même est un clin d'œil au film *Spaceballs* de Mel Brooks (1987), d'où vient aussi le mème « Ludicrous speed » de la section 5 ; cette image provient du blogue [efb28.wordpress.com](https://efb28.wordpress.com/wp-content/uploads/2014/04/spaceballs_ludicrous_speed.jpg), et nous l'affichons à travers le cache d'images de Brave Search.

Notre action de diagnostic s'inspire de l'action [`TejasvOnly/random-rickroll`](https://github.com/TejasvOnly/random-rickroll) de Tejasv Sharma, publiée sous licence Apache 2.0. Nous en reprenons l'idée, le paramètre `percentage`, les messages « Gottem!! » et « Fine, you win this time. », ainsi que l'animation, hébergée sur `user-images.githubusercontent.com`. La vidéo vers laquelle pointe le diagnostic est hébergée sur YouTube ; par respect pour la blague, nous laissons au lecteur le plaisir de découvrir de quel clip de 1987 il s'agit.

Pour les règles et le workflow, nous nous sommes appuyés sur la [documentation de Semgrep](https://semgrep.dev/docs/), en particulier les pages sur la syntaxe des règles, le mode taint et l'option `--baseline-commit`, ainsi que sur le jeu de règles public [`p/java`](https://semgrep.dev/p/java) et sur nos propres exemples de la tâche 1 ([github.com/Toky5/-semgrep-ift3913](https://github.com/Toky5/-semgrep-ift3913)). Les catégories de vulnérabilités suivent la liste [CWE](https://cwe.mitre.org/) de MITRE et le [Top 10 de l'OWASP](https://owasp.org/Top10/). Le code modifié est celui d'[Apache Tika](https://github.com/apache/tika), sous licence Apache 2.0, et la tâche suit l'énoncé du cours IFT3913 de l'Université de Montréal.
