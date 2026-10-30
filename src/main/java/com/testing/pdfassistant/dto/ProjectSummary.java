package com.testing.pdfassistant.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

public record ProjectSummary(
        @JsonPropertyDescription("Le numéro et le titre exact du projet (ex: Projet X : Titre)")
        String numeroOuTitre,

        @JsonPropertyDescription("Le nom complet des personnes clientes ET leur organisme entre parenthèses")
        String client,

        @JsonPropertyDescription("Le nom de l'encadrant et son établissement")
        String encadrant,

        @JsonPropertyDescription("Résumé précis de l'objectif et du problème à résoudre pour ce projet")
        String objectifPrincipal,

        @JsonPropertyDescription("Liste exhaustive de tous les outils, langages, frameworks, plugins et compétences techniques cités dans ce projet")
        List<String> motsClesOuTechnologies){
}
