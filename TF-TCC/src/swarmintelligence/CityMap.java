package swarmintelligence;

import java.awt.*;
import java.util.*;

/**
 * Representa un mapa de ciudad con cuadriculas de calles
 * Cuadras de 20x20 unidades
 */
public class CityMap {
    public static final int BLOCK_SIZE = 20;
    public static final int STREET_WIDTH = 2;
    public static final int BLOCKS_X = 30; // 30 cuadras horizontales
    public static final int BLOCKS_Y = 20; // 20 cuadras verticales
    public static final int WORLD_WIDTH = BLOCKS_X * BLOCK_SIZE;
    public static final int WORLD_HEIGHT = BLOCKS_Y * BLOCK_SIZE;
    
    /**
     * Representa una interseccion (esquina) donde pueden estar vehiculos y clientes
     */
    public static class Intersection {
        public final int gridX; // coordenada en grid
        public final int gridY;
        public final int worldX; // coordenada en mundo
        public final int worldY;
        
        public Intersection(int gridX, int gridY) {
            this.gridX = gridX;
            this.gridY = gridY;
            this.worldX = gridX * BLOCK_SIZE;
            this.worldY = gridY * BLOCK_SIZE;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Intersection)) return false;
            Intersection that = (Intersection) o;
            return gridX == that.gridX && gridY == that.gridY;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(gridX, gridY);
        }
    }
    
    /**
     * Obtiene una interseccion aleatoria valida
     */
    public static Intersection getRandomIntersection(Random rnd) {
        int gx = rnd.nextInt(BLOCKS_X + 1);
        int gy = rnd.nextInt(BLOCKS_Y + 1);
        return new Intersection(gx, gy);
    }
    
    /**
     * Calcula distancia Manhattan entre dos intersecciones (simulando recorrido por calles)
     */
    public static int getManhattanDistance(Intersection a, Intersection b) {
        return Math.abs(a.gridX - b.gridX) + Math.abs(a.gridY - b.gridY);
    }
    
    /**
     * Calcula la distancia en unidades del mundo
     */
    public static double getWorldDistance(Intersection a, Intersection b) {
        return getManhattanDistance(a, b) * BLOCK_SIZE;
    }
    
    /**
     * Calcula el camino entre dos intersecciones (movimiento por calles)
     */
    public static java.util.List<Intersection> calculatePath(Intersection start, Intersection end) {
        java.util.List<Intersection> path = new ArrayList<>();
        path.add(start);
        
        int currentX = start.gridX;
        int currentY = start.gridY;
        
        // Primero moverse horizontalmente
        while (currentX != end.gridX) {
            currentX += (currentX < end.gridX) ? 1 : -1;
            path.add(new Intersection(currentX, currentY));
        }
        
        // Luego moverse verticalmente
        while (currentY != end.gridY) {
            currentY += (currentY < end.gridY) ? 1 : -1;
            path.add(new Intersection(currentX, currentY));
        }
        
        return path;
    }
    
    /**
     * Convierte coordenadas del mundo a la interseccion mas cercana
     */
    public static Intersection worldToIntersection(int worldX, int worldY) {
        int gx = Math.round((float) worldX / BLOCK_SIZE);
        int gy = Math.round((float) worldY / BLOCK_SIZE);
        gx = Math.max(0, Math.min(BLOCKS_X, gx));
        gy = Math.max(0, Math.min(BLOCKS_Y, gy));
        return new Intersection(gx, gy);
    }
}
