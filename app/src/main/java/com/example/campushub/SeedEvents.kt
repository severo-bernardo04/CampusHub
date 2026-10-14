package com.example.campushub

import android.util.Log
import com.example.campushub.model.Event
import com.google.firebase.firestore.FirebaseFirestore

object SeedEvents {

    private val events = listOf(
        Event(
            title = "Semana da Computação",
            description = "Palestras, oficinas e maratona de programação com profissionais da área.",
            date = "10/11/2026 - 19h",
            location = "Auditório Central",
            hostCourse = "Ciência da Computação",
            isPublic = true
        ),
        Event(
            title = "Workshop de Segurança da Informação",
            description = "Introdução a pentest e boas práticas. Vagas exclusivas para o curso.",
            date = "12/11/2026 - 14h",
            location = "Laboratório 3",
            hostCourse = "Ciência da Computação",
            isPublic = false
        ),
        Event(
            title = "Feira de Engenharia Civil",
            description = "Exposição de maquetes e projetos dos alunos, aberta à comunidade.",
            date = "17/11/2026 - 9h",
            location = "Pátio Principal",
            hostCourse = "Engenharia Civil",
            isPublic = true
        ),
        Event(
            title = "Visita Técnica: Obra Residencial",
            description = "Visita acompanhada por engenheiros. Apenas alunos de Engenharia Civil.",
            date = "19/11/2026 - 8h",
            location = "Saída do Bloco B",
            hostCourse = "Engenharia Civil",
            isPublic = false
        ),
        Event(
            title = "Palestra de Empreendedorismo",
            description = "Como transformar uma ideia em negócio, com cases de startups locais.",
            date = "24/11/2026 - 20h",
            location = "Auditório B",
            hostCourse = "Administração",
            isPublic = true
        ),
        Event(
            title = "Simulado de Júri",
            description = "Simulação de júri popular. Exclusivo para estudantes de Direito.",
            date = "26/11/2026 - 18h30",
            location = "Sala de Júri",
            hostCourse = "Direito",
            isPublic = false
        ),
        Event(
            title = "Campanha de Saúde e Prevenção",
            description = "Aferição de pressão, orientações e atividades abertas ao público.",
            date = "01/12/2026 - 9h",
            location = "Praça de Convivência",
            hostCourse = "Medicina",
            isPublic = true
        ),
        Event(
            title = "Confraternização de Fim de Ano",
            description = "Encerramento do semestre com todos os cursos.",
            date = "10/12/2026 - 19h",
            location = "Ginásio",
            hostCourse = "Outro",
            isPublic = true
        )
    )

    fun run(db: FirebaseFirestore, onDone: () -> Unit) {
        db.collection("events").limit(1).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    onDone()
                    return@addOnSuccessListener
                }

                val batch = db.batch()
                events.forEach { event ->
                    // document() sem argumento gera um ID aleatório único
                    batch.set(db.collection("events").document(), event)
                }
                batch.commit()
                    .addOnFailureListener { Log.e("SEED", "Falha ao gravar", it) }
                    .addOnCompleteListener { onDone() }
            }
            .addOnFailureListener {
                Log.e("SEED", "Falha ao ler", it)
                onDone()
            }
    }
}