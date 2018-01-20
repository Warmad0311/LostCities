package mcjty.lostcities.dimensions.world.lost;

import mcjty.lostcities.dimensions.world.LostCityChunkGenerator;
import mcjty.lostcities.varia.ChunkCoord;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class CitySphere {

    private static Map<ChunkCoord, CitySphere> citySphereCache = new HashMap<>();

    private final boolean enabled;

    private boolean monorailNorthCandidate;
    private boolean monorailSouthCandidate;
    private boolean monorailWestCandidate;
    private boolean monorailEastCandidate;

    private char glassBlock;
    private char baseBlock;
    private char sideBlock;

    private CitySphere(boolean enabled) {
        this.enabled = enabled;
    }

    public void setBlocks(char glassBlock, char baseBlock, char sideBlock) {
        this.glassBlock = glassBlock;
        this.baseBlock = baseBlock;
        this.sideBlock = sideBlock;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public char getGlassBlock() {
        return glassBlock;
    }

    public char getBaseBlock() {
        return baseBlock;
    }

    public char getSideBlock() {
        return sideBlock;
    }

    public static void cleanCache() {
        citySphereCache.clear();
    }

    /**
     * Return true if there is a horizontal monorail here. This is the case if this chunk is on a city center multiple
     * (i.e. multiple of 16) and if there are cities left and right that both want a monorail in the correct direction
     */
    public static boolean hasHorizontalMonorail(int chunkX, int chunkZ, LostCityChunkGenerator provider) {
        if ((chunkZ & 0xf) == 8) {
            // There is a city center on this vertical chunk coordinate
            // Find the first city on the right (with a limit)
            // @todo optimize this by caching the result of this scan?
            boolean result = false;
            for (int cx = chunkX+1 ; cx < chunkX+64 ; cx++) {
                if ((cx & 0xf) == 8) {
                    // This could be a city center
                    CitySphere sphere = getCitySphere(cx, chunkZ, provider);
                    if (sphere.isEnabled()) {
                        if (!sphere.monorailWestCandidate) {
                            return false;
                        } else {
                            result = true;
                            break;  // No need to continue. We found our city
                        }
                    }
                }
            }
            if (!result) {
                return false;
            }
            result = false;
            // Find the first city on the left (with a limit)
            for (int cx = chunkX-1 ; cx > chunkX-64 ; cx--) {
                if ((cx & 0xf) == 8) {
                    // This could be a city center
                    CitySphere sphere = getCitySphere(cx, chunkZ, provider);
                    if (sphere.isEnabled()) {
                        if (!sphere.monorailEastCandidate) {
                            return false;
                        } else {
                            result = true;
                            break;  // No need to continue. We found our city
                        }
                    }
                }
            }
            return result;
        } else {
            return false;
        }
    }

    public static boolean hasVerticalMonorail(int chunkX, int chunkZ, LostCityChunkGenerator provider) {
        if ((chunkX & 0xf) == 8) {
            boolean result = false;
            for (int cz = chunkZ+1 ; cz < chunkZ+64 ; cz++) {
                if ((cz & 0xf) == 8) {
                    // This could be a city center
                    CitySphere sphere = getCitySphere(chunkX, cz, provider);
                    if (sphere.isEnabled()) {
                        if (!sphere.monorailNorthCandidate) {
                            return false;
                        } else {
                            result = true;
                            break;  // No need to continue. We found our city
                        }
                    }
                }
            }
            if (!result) {
                return false;
            }
            result = false;
            for (int cz = chunkZ-1 ; cz > chunkZ-64 ; cz--) {
                if ((cz & 0xf) == 8) {
                    // This could be a city center
                    CitySphere sphere = getCitySphere(chunkX, cz, provider);
                    if (sphere.isEnabled()) {
                        if (!sphere.monorailSouthCandidate) {
                            return false;
                        } else {
                            result = true;
                            break;  // No need to continue. We found our city
                        }
                    }
                }
            }
            return result;
        } else {
            return false;
        }
    }

    /**
     * chunkX and chunkZ must be the city sphere center
     */
    public static float getSphereRadius(int chunkX, int chunkZ, LostCityChunkGenerator provider) {
        return City.getCityRadius(chunkX, chunkZ, provider) * provider.profile.CITYSPHERE_FACTOR;
    }


    /**
     * Return true if a given chunk is fully enclosed in a city sphere
     */
    public static boolean isEnclosed(int chunkX, int chunkZ, LostCityChunkGenerator provider) {
        ChunkCoord cityCenter = CitySphere.getCityCenterForSpace(chunkX, chunkZ, provider);
        CitySphere sphere = getCitySphereAtCenter(cityCenter, provider);
        if (!sphere.isEnabled()) {
            return false;
        }
        float radius = CitySphere.getSphereRadius(cityCenter.getChunkX(), cityCenter.getChunkZ(), provider);
        double sqradiusOffset = (radius-2) * (radius-2);
        int cx = cityCenter.getChunkX() * 16 + 8;
        int cz = cityCenter.getChunkZ() * 16 + 8;
        if (squaredDistance(cx, cz, chunkX*16, chunkZ*16) > sqradiusOffset) {
            return false;
        }
        if (squaredDistance(cx, cz, chunkX*16+15, chunkZ*16) > sqradiusOffset) {
            return false;
        }
        if (squaredDistance(cx, cz, chunkX*16, chunkZ*16+15) > sqradiusOffset) {
            return false;
        }
        if (squaredDistance(cx, cz, chunkX*16+15, chunkZ*16+15) > sqradiusOffset) {
            return false;
        }
        return true;
    }

    private static double squaredDistance(int cx, int cz, int x, int z) {
        return (cx-x)*(cx-x) + (cz-z)*(cz-z);
    }

    /**
     * Return a (possibly cached) city sphere which affects this chunk. That means it will
     * go find the city sphere center chunk and get data from that
     */
    @Nonnull
    public static CitySphere getCitySphere(int chunkX, int chunkZ, LostCityChunkGenerator provider) {
        ChunkCoord center = getCityCenterForSpace(chunkX, chunkZ, provider);
        return getCitySphereAtCenter(center, provider);
    }

    /**
     * Return a (possibly cached) city sphere for this city center chunk
     */
    public static CitySphere getCitySphereAtCenter(ChunkCoord center, LostCityChunkGenerator provider) {
        int chunkX;
        int chunkZ;
        if (!citySphereCache.containsKey(center)) {
            chunkX = center.getChunkX();
            chunkZ = center.getChunkZ();
            Random rand = new Random(provider.seed + chunkX * 961744153L + chunkZ * 837971201L);
            rand.nextFloat();
            rand.nextFloat();
            CitySphere citySphere;
            // This information is for city spheres. This information is only relevant
            // in the chunk representing the center of the city
            if (isCitySphereCenterCandidate(chunkX, chunkZ)) {
                boolean enabled = rand.nextFloat() < provider.profile.CITYSPHERE_CHANCE;
                citySphere = new CitySphere(enabled);
                if (enabled) {
                    citySphere.monorailNorthCandidate = rand.nextFloat() < provider.profile.CITYSPHERE_MONORAIL_CHANCE;
                    citySphere.monorailSouthCandidate = rand.nextFloat() < provider.profile.CITYSPHERE_MONORAIL_CHANCE;
                    citySphere.monorailWestCandidate = rand.nextFloat() < provider.profile.CITYSPHERE_MONORAIL_CHANCE;
                    citySphere.monorailEastCandidate = rand.nextFloat() < provider.profile.CITYSPHERE_MONORAIL_CHANCE;
                }
            } else {
                citySphere = new CitySphere(false);
            }
            citySphereCache.put(center, citySphere);
        }
        return citySphereCache.get(center);
    }

    /**
     * This returns the center of the city in case we're in a space type world
     */
    public static ChunkCoord getCityCenterForSpace(int chunkX, int chunkZ, LostCityChunkGenerator provider) {
        int cx = (chunkX & ~0xf) + 8;
        int cz = (chunkZ & ~0xf) + 8;
        return new ChunkCoord(provider.dimensionId, cx, cz);
    }

    /**
     * Return true if this coordinate is potentially a candidate to be a city center
     */
    public static boolean isCitySphereCenterCandidate(int chunkX, int chunkZ) {
        return (chunkX & 0xf) == 8 && (chunkZ & 0xf) == 8;
    }

}