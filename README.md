# CampusHub

Aplicativo Android onde alunos da universidade podem visualizar eventos do campus, consultar detalhes e se inscrever neles. Cada evento é hospedado por um curso e pode ser **aberto ao público** ou **fechado ao curso** que o promove.

Projeto acadêmico desenvolvido em **Kotlin + XML**, usando **Firebase Authentication** e **Cloud Firestore**.

---

## Funcionalidades

- Cadastro de conta (nome, e-mail, senha e curso)
- Login e logout (a sessão persiste entre aberturas do app)
- Recuperação de senha por e-mail (fluxo nativo do Firebase)
- Edição de perfil (nome e curso)
- Listagem de eventos com filtros:
  - por curso que hospeda o evento
  - por tipo: todos, abertos ao público ou do meu curso
- Detalhes do evento (título, descrição, data, local, curso, tipo de acesso)
- Inscrição e cancelamento de inscrição
- Regra de acesso: eventos fechados só permitem inscrição de alunos do curso anfitrião
- Tela "Meus Eventos" com as inscrições do usuário

---

## Tecnologias

| Item | Uso |
|------|-----|
| Kotlin | Linguagem do app |
| XML (Views) | Interface |
| RecyclerView | Listas de eventos |
| Firebase Authentication | Cadastro, login e recuperação de senha |
| Cloud Firestore | Perfis, eventos e inscrições |
| Gradle (Kotlin DSL) | Build |

---

## Estrutura do projeto

```
CampusHub/
├── .gitignore
├── README.md
└── app/
    ├── build.gradle.kts
    ├── google-services.json            # NÃO versionado (veja "Como rodar")
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/example/campushub/
        │   ├── model/
        │   │   ├── AppUser.kt          # Modelo do usuário
        │   │   └── Event.kt            # Modelo do evento
        │   ├── LoginActivity.kt
        │   ├── RegisterActivity.kt
        │   ├── ForgotPasswordActivity.kt
        │   ├── EventsListActivity.kt   # Lista + filtros
        │   ├── EventDetailActivity.kt  # Detalhes + inscrição/cancelamento
        │   ├── MyEventsActivity.kt
        │   ├── ProfileActivity.kt      # Edição de perfil + logout
        │   ├── EventAdapter.kt         # Adapter do RecyclerView
        │   └── SimpleSelectListener.kt # Helper para os Spinners
        └── res/
            ├── drawable/   bg_input.xml, bg_button.xml
            ├── layout/     activity_*.xml, item_event.xml
            └── values/     colors.xml, strings.xml, arrays.xml, themes.xml
```

---

## Modelo de dados (Firestore)

```
users/{uid}
  name:   String
  email:  String
  course: String

events/{eventId}
  title:       String
  description: String
  date:        String
  location:    String
  hostCourse:  String    // curso que hospeda o evento
  isPublic:    Boolean   // true = aberto ao público | false = fechado ao curso

users/{uid}/enrollments/{eventId}
  eventId: String        // o documento existir = usuário inscrito
```

O ID do documento em `enrollments` é o próprio `eventId`, então verificar se o aluno já está inscrito é só checar se o documento existe.

---

## Como rodar

### Pré-requisitos

- Android Studio (versão recente)
- JDK 17 (já incluso no Android Studio)
- Conta Google para acessar o Firebase Console
- Emulador ou celular Android (API 24 ou superior)

### 1. Clonar o repositório

```bash
git clone <url-do-repositorio>
cd CampusHub
```

Abra a pasta no Android Studio e aguarde o primeiro sync do Gradle.

### 2. Criar o projeto no Firebase

1. Acesse <https://console.firebase.google.com> e clique em **Criar um projeto**.
2. Registre um app Android com o package name **`com.example.campushub`** (deve ser idêntico ao `applicationId` do `app/build.gradle.kts`).
3. Baixe o **`google-services.json`** e coloque na pasta **`app/`** (mesmo nível do `build.gradle.kts` do módulo, não dentro de `src/`).

> O `google-services.json` está no `.gitignore` e não vem no repositório. Cada pessoa que clonar o projeto precisa gerar o seu próprio no Console, ou recebê-lo separadamente do autor.

### 3. Habilitar a autenticação

No Console: **Build > Authentication > Sign-in method > E-mail/senha** → ativar.

### 4. Criar o banco Firestore

No Console: **Build > Firestore Database > Criar banco de dados** (modo de teste para desenvolvimento).

Depois, na aba **Regras**, cole:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /events/{eventId} {
      allow read: if request.auth != null;
      allow write: if false;
    }
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
      match /enrollments/{eventId} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
    }
  }
}
```

### 5. Cadastrar eventos de teste

O app não tem tela de criação de eventos. Crie-os manualmente em **Firestore Database > Iniciar coleção** com o ID `events`. Exemplo de documento:

| Campo | Tipo | Exemplo |
|-------|------|---------|
| title | string | Semana da Computação |
| description | string | Palestras e oficinas sobre tecnologia |
| date | string | 15/11/2026 - 19h |
| location | string | Auditório Central |
| hostCourse | string | Ciência da Computação |
| isPublic | boolean | true |

O valor de `hostCourse` deve ser igual a um dos itens de `res/values/arrays.xml` (`course_list`) para que a regra de "evento fechado ao curso" funcione.

### 6. Dependências do Gradle

Confirme que o `app/build.gradle.kts` contém:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}
```

E o `build.gradle.kts` da raiz:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    id("com.google.gms.google-services") version "4.5.0" apply false
}
```

### 7. Executar

Clique em **Sync Project with Gradle Files** e depois em **Run**. Crie uma conta no app, escolha um curso e navegue pelos eventos cadastrados.

---

## Regras de negócio

- **Evento aberto ao público** (`isPublic = true`): qualquer aluno pode se inscrever.
- **Evento fechado ao curso** (`isPublic = false`): só se inscreve quem tem o mesmo curso do `hostCourse`. Para os demais, o botão fica desabilitado.
- A inscrição pode ser cancelada a qualquer momento na tela de detalhes.

---

## Limitações conhecidas

- Não há tela para criar ou editar eventos (feito direto no Console).
- A tela "Meus Eventos" usa `whereIn`, que aceita no máximo 10 IDs por consulta no Firestore.
- O e-mail é exibido no perfil, mas não pode ser alterado (exigiria reautenticação).
- Sem validação de campos nem tratamento de erros nas telas, por escolha de escopo (versão básica).
- A regra de acesso a eventos fechados é verificada apenas no app; para proteger no servidor seria necessário reforçar as regras do Firestore.

---

## Possíveis melhorias

- Tela de criação de eventos para administradores
- Validação de formulários e mensagens de erro
- Limite de vagas por evento
- Busca por título
- Datas em formato `Timestamp` com ordenação cronológica
