package org.example;

import org.example.entities.Categorie;
import org.example.entities.Commande;
import org.example.entities.Produit;
import org.example.service.CrudService;
import org.example.service.ProduitService;
import org.example.util.HibernateUtil;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;

public class App {
    public static void main(String[] args) {
        CrudService<Categorie, Long> catService = new CrudService<>(Categorie.class);
        ProduitService produitService = new ProduitService();
        CrudService<Commande, Long> commandeService = new CrudService<>(Commande.class);

        // Nom unique à chaque lancement (la colonne nom est unique)
        Categorie cat = new Categorie("Informatique " + System.currentTimeMillis());
        catService.create(cat);
        System.out.println("Catégorie créée avec l'ID : " + cat.getId());

        // Deux produits
        Produit p1 = new Produit("Souris", new BigDecimal("19.99"), cat);
        Produit p2 = new Produit("Écran", new BigDecimal("149.90"), cat);
        produitService.create(p1);
        produitService.create(p2);

        // Commande : un seul EntityManager, les produits sont rechargés dans la même session
        EntityManager emCmd = HibernateUtil.getSessionFactory().createEntityManager();
        EntityTransaction txCmd = emCmd.getTransaction();
        try {
            txCmd.begin();
            Produit prod1 = emCmd.find(Produit.class, p1.getId());
            Produit prod2 = emCmd.find(Produit.class, p2.getId());

            Commande cmd = new Commande();
            cmd.setDateCommande(LocalDate.now());
            cmd.addProduit(prod1, 3);
            cmd.addProduit(prod2, 5);

            emCmd.persist(cmd);
            txCmd.commit();
            System.out.println("Commande créée avec ID = " + cmd.getId());
        } catch (Exception e) {
            if (txCmd.isActive()) txCmd.rollback();
            throw e;
        } finally {
            emCmd.close();
        }

        // Transaction manuelle : tout ou rien
        EntityManager em = HibernateUtil.getSessionFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Produit p = new Produit("Clavier", new BigDecimal("49.99"), catService.getById(cat.getId()));
            em.persist(p);
            if (p.getPrix().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Prix négatif");
            }
            tx.commit();
            System.out.println("Clavier enregistré");
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            System.err.println("Transaction annulée : " + e.getMessage());
        } finally {
            em.close();
        }

        HibernateUtil.shutdown();
    }
}