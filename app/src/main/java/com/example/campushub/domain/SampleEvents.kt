package com.example.campushub.domain

import com.example.campushub.model.Event
import java.util.Calendar

/**
 * Eventos de exemplo em locais reais de Santiago-RS.
 * URI: ponto geodésico dentro do campus (coordenada precisa).
 * Centro e Zamperetti: coordenadas aproximadas; o organizador pode refinar em "Buscar no mapa".
 */
object SampleEvents {

    private const val URI_PLACE = "URI Câmpus Santiago"
    private const val URI_ADDRESS = "Av. Batista Bonotto Sobrinho, 733 - São Vicente, Santiago - RS"
    private const val URI_LAT = -29.18995
    private const val URI_LNG = -54.88577

    private const val CENTER_LAT = -29.1897
    private const val CENTER_LNG = -54.8666

    private const val PARK_LAT = -29.2024
    private const val PARK_LNG = -54.8604

    private fun at(daysFromNow: Int, hour: Int, minute: Int, now: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, daysFromNow)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun event(
        organizerId: String, title: String, description: String, category: String,
        course: String, isPublic: Boolean, capacity: Int, dateMillis: Long,
        place: String, address: String, lat: Double, lng: Double
    ) = Event(
        title = title, description = description, category = category,
        hostCourse = course, isPublic = isPublic, dateMillis = dateMillis,
        location = place, address = address, latitude = lat, longitude = lng,
        organizerId = organizerId, status = EventStatus.PUBLISHED, capacity = capacity
    )

    /** 7 eventos próximos e 4 encerrados, com datas relativas a [now]. */
    fun build(now: Long = System.currentTimeMillis(), organizerId: String = ""): List<Event> = listOf(
        // Próximos
        event(
            organizerId, "Semana da Computação",
            "Palestras, oficinas e maratona de programação com profissionais da área.",
            "Palestra", "Ciência da Computação", true, 80, at(7, 19, 0, now),
            URI_PLACE, URI_ADDRESS, URI_LAT, URI_LNG
        ),
        event(
            organizerId, "Workshop de Segurança da Informação",
            "Introdução a pentest e boas práticas. Vagas exclusivas para o curso.",
            "Workshop", "Ciência da Computação", false, 30, at(10, 14, 0, now),
            URI_PLACE, URI_ADDRESS, URI_LAT, URI_LNG
        ),
        event(
            organizerId, "Feira de Engenharia Civil",
            "Exposição de maquetes e projetos dos alunos, aberta à comunidade.",
            "Feira", "Engenharia Civil", true, 150, at(14, 9, 0, now),
            URI_PLACE, URI_ADDRESS, URI_LAT, URI_LNG
        ),
        event(
            organizerId, "Visita Técnica: Obra Residencial",
            "Visita acompanhada por engenheiros. Apenas alunos de Engenharia Civil.",
            "Visita Técnica", "Engenharia Civil", false, 25, at(18, 8, 0, now),
            "Saída da URI Câmpus Santiago", URI_ADDRESS, URI_LAT, URI_LNG
        ),
        event(
            // Capacidade pequena de propósito: dá para testar a lista de espera rapidamente
            organizerId, "Mentoria de Carreira com Egressos",
            "Conversa em pequeno grupo com egressos sobre mercado de trabalho.",
            "Workshop", "Administração", true, 2, at(21, 19, 30, now),
            URI_PLACE, URI_ADDRESS, URI_LAT, URI_LNG
        ),
        event(
            organizerId, "Noite Cultural",
            "Apresentações de música, dança e teatro feitas pelos alunos.",
            "Cultural", "Outro", true, 120, at(30, 19, 0, now),
            "Estação do Conhecimento",
            "Estação do Conhecimento (antiga estação ferroviária) - Centro, Santiago - RS",
            CENTER_LAT, CENTER_LNG
        ),
        event(
            organizerId, "Caminhada Ecológica no Parque Zamperetti",
            "Trilha guiada com educação ambiental, aberta à comunidade.",
            "Esporte", "Outro", true, 60, at(40, 8, 0, now),
            "Parque Ambiental Zamperetti",
            "Parque Ambiental Zamperetti - Bairro Zamperetti, Santiago - RS",
            PARK_LAT, PARK_LNG
        ),
        // Encerrados
        event(
            organizerId, "Aula Inaugural do Semestre",
            "Abertura do semestre com apresentação das atividades do campus.",
            "Palestra", "Outro", true, 200, at(-20, 19, 0, now),
            URI_PLACE, URI_ADDRESS, URI_LAT, URI_LNG
        ),
        event(
            organizerId, "Campanha de Saúde e Prevenção",
            "Aferição de pressão, orientações e atividades abertas ao público.",
            "Social", "Medicina", true, 100, at(-12, 9, 0, now),
            "Praça Moisés Viana", "Rua Pinheiro Machado, 1939 - Centro, Santiago - RS",
            CENTER_LAT, CENTER_LNG
        ),
        event(
            organizerId, "Simulado de Júri",
            "Simulação de júri popular. Exclusivo para estudantes de Direito.",
            "Workshop", "Direito", false, 40, at(-5, 18, 30, now),
            URI_PLACE, URI_ADDRESS, URI_LAT, URI_LNG
        ),
        event(
            organizerId, "Torneio de Xadrez",
            "Torneio aberto com partidas rápidas e premiação simbólica.",
            "Esporte", "Outro", true, 32, at(-3, 14, 0, now),
            "Museu Municipal Pedro Palmeiro",
            "Rua Pinheiro Machado, 2191 - 2º andar, Centro, Santiago - RS",
            CENTER_LAT, CENTER_LNG
        )
    )
}