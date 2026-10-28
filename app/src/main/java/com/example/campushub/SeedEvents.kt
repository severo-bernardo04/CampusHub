package com.example.campushub

import android.util.Log
import com.example.campushub.model.Event
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

object SeedEvents {

    private fun at(daysFromNow: Int, hour: Int, minute: Int = 0): Long =
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, daysFromNow)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun event(
        title: String, description: String, category: String, course: String,
        isPublic: Boolean, dateMillis: Long,
        place: String, address: String, lat: Double, lng: Double
    ) = Event(
        title = title, description = description, category = category,
        hostCourse = course, isPublic = isPublic, dateMillis = dateMillis,
        location = place, address = address, latitude = lat, longitude = lng
    )

    private fun buildEvents(): List<Event> = listOf(
        // Próximos
        event(
            "Semana da Computação",
            "Palestras, oficinas e maratona de programação com profissionais da área.",
            "Palestra", "Ciência da Computação", true, at(7, 19),
            "Auditório Central", "Rua Exemplo, 100 - Centro, Santiago - RS", -29.1892, -54.8650
        ),
        event(
            "Workshop de Segurança da Informação",
            "Introdução a pentest e boas práticas. Vagas exclusivas para o curso.",
            "Workshop", "Ciência da Computação", false, at(10, 14),
            "Laboratório 3", "Rua Exemplo, 100 - Bloco B, Santiago - RS", -29.1897, -54.8655
        ),
        event(
            "Feira de Engenharia Civil",
            "Exposição de maquetes e projetos dos alunos, aberta à comunidade.",
            "Feira", "Engenharia Civil", true, at(14, 9),
            "Pátio Principal", "Rua Exemplo, 100 - Centro, Santiago - RS", -29.1901, -54.8662
        ),
        event(
            "Visita Técnica: Obra Residencial",
            "Visita acompanhada por engenheiros. Apenas alunos de Engenharia Civil.",
            "Visita Técnica", "Engenharia Civil", false, at(18, 8),
            "Saída do Bloco B", "Av. Exemplo, 250 - Centro, Santiago - RS", -29.1915, -54.8689
        ),
        event(
            "Palestra de Empreendedorismo",
            "Como transformar uma ideia em negócio, com cases de startups locais.",
            "Palestra", "Administração", true, at(21, 20),
            "Auditório B", "Rua Exemplo, 100 - Centro, Santiago - RS", -29.1894, -54.8648
        ),
        event(
            "Noite Cultural",
            "Apresentações de música, dança e teatro feitas pelos alunos.",
            "Cultural", "Outro", true, at(30, 19),
            "Praça de Convivência", "Rua Exemplo, 100 - Centro, Santiago - RS", -29.1899, -54.8658
        ),
        event(
            "Campeonato de Futsal",
            "Torneio entre turmas, com premiação para as três melhores equipes.",
            "Esporte", "Outro", true, at(40, 15),
            "Ginásio Municipal", "Av. Exemplo, 800 - Santiago - RS", -29.1930, -54.8700
        ),
        // Encerrados
        event(
            "Aula Inaugural do Semestre",
            "Abertura do semestre com apresentação das atividades do campus.",
            "Palestra", "Outro", true, at(-20, 19),
            "Auditório Central", "Rua Exemplo, 100 - Centro, Santiago - RS", -29.1892, -54.8650
        ),
        event(
            "Campanha de Saúde e Prevenção",
            "Aferição de pressão, orientações e atividades abertas ao público.",
            "Social", "Medicina", true, at(-12, 9),
            "Praça de Convivência", "Rua Exemplo, 100 - Centro, Santiago - RS", -29.1899, -54.8658
        ),
        event(
            "Simulado de Júri",
            "Simulação de júri popular. Exclusivo para estudantes de Direito.",
            "Workshop", "Direito", false, at(-5, 18, 30),
            "Sala de Júri", "Rua Exemplo, 100 - Bloco C, Santiago - RS", -29.1890, -54.8645
        ),
        event(
            "Torneio de Xadrez",
            "Torneio aberto com partidas rápidas e premiação simbólica.",
            "Esporte", "Outro", true, at(-3, 14),
            "Sala 12", "Rua Exemplo, 100 - Bloco A, Santiago - RS", -29.1895, -54.8652
        )
    )

    /**
     * Popula a coleção "events" somente quando ela está vazia.
     * Cada documento recebe um ID único gerado pelo Firestore.
     */
    fun run(db: FirebaseFirestore, uid: String?, onDone: () -> Unit) {
        db.collection("events").limit(1).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    onDone()
                    return@addOnSuccessListener
                }

                val now = System.currentTimeMillis()
                val batch = db.batch()
                buildEvents().forEach { event ->
                    val ref = db.collection("events").document()
                    batch.set(ref, event)

                    if (uid != null && event.isPublic && event.dateMillis < now) {
                        batch.set(
                            db.collection("users").document(uid)
                                .collection("enrollments").document(ref.id),
                            mapOf("eventId" to ref.id)
                        )
                    }
                    // --------------------------------------------------------
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