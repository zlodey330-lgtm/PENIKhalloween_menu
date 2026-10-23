# Halloween Menu (Fabric 1.21.1)

Хеллоуинское главное меню: живая 3D-сцена (кладбище, луна, фонари, тыквы, мыши, ведьма),
кнопки Играть / Одиночная игра / Сеть / Моды / Настройки / Выйти / Ванильное меню,
плеер сверху (2 трека по кругу: 1 -> 2 -> 1 -> ...).

## Как собрать .jar
Нужны: Java 21 (JDK) и интернет.

Windows:   gradlew.bat build
Linux/Mac: ./gradlew build

Готовый файл: build/libs/halloween_menu-1.0.0.jar

## Как поставить
Положи jar в папку mods вместе с Fabric API (для 1.21.1). Для кнопки «Моды» нужен Mod Menu.

## Без компьютера с Java
Залей папку на GitHub: вкладка Actions -> build соберёт jar сама (файл .github/workflows/build.yml).

## Где что менять
- Названия треков: HalloweenMusic.java (массив NAMES)
- Музыка: src/main/resources/assets/halloween_menu/sounds/track1.ogg и track2.ogg
- Сцена (цвета, расстановка, ведьма): Scene3D.java
- Кнопки и их действия: HalloweenScreen.java, метод layout()
