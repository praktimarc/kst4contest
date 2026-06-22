package kst4contest.view.map;

public class mapTest {

    public static void main(String[] args) {

        TerrainPackageService terrainPackageService = new TerrainPackageService();

        TerrainPackageService.TerrainPreparationResult result =
                terrainPackageService.prepareTerrainForLocators(
                        "JO51IJ",
                        "JO22JK",
                        "TEST",
                        144.300,
                        10.0,
                        10.0,
                        ""
                );

        System.out.println(result.success());
        System.out.println(result.message());

    }
}
