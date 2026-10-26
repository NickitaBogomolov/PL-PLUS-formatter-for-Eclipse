# PL+ Formatter for Eclipse

Плагин для форматирования кода PL/SQL (PL+) в Eclipse. Поддерживает файлы `.plp`, а также общие расширения PL/SQL (`.plsql`, `.pls`, `.sql`).

Цель плагина — привести код к единообразному стилю, упростить чтение и автоматизировать форматирование при сохранении файла.

## Возможности

- форматирование кода PL+
- ручная команда форматирования
- автоматическое форматирование при сохранении файлов `.plp`
- настройка размера отступа
- поддержка стандартных расширений PL/SQL

## Требования

- Eclipse IDE для Java Developers
- Eclipse Plug-in Development Tools (PDE)
- Java 17+
- Eclipse 2022-09 или новее

## Установка из открытого репозитория GitHub

Самый простой способ для обычного пользователя — скачать ZIP-архив репозитория и установить плагин через Eclipse.

### Шаг 1. Скачайте архив репозитория

1. Откройте репозиторий на GitHub.
2. Нажмите кнопку `Code`.
3. Выберите `Download ZIP`.
4. Сохраните ZIP-файл на компьютер.

### Шаг 2. Установите плагин в Eclipse

1. Откройте Eclipse.
2. Перейдите в меню:
   `Help -> Install New Software`
3. Нажмите кнопку `Add...`.
4. Выберите `Archive...`.
5. Укажите скачанный ZIP проекта.
6. В списке доступных компонентов выберите `PL+ Formatter`.
7. Нажмите `Next`.
8. Подтвердите установку и дождитесь завершения.
9. Перезапустите Eclipse.

### Шаг 3. Включите автоформатирование

1. Откройте:
   `Window -> Preferences`
2. Перейдите в раздел:
   `PL+ Formatter`
3. Включите опцию:
   `Форматировать файл при сохранении (*.plp)`
4. Сохраните настройки.

## Использование

После установки плагина:

1. Откройте файл `.plp` в Eclipse.
2. Используйте команду форматирования вручную или просто сохраните файл.
3. Если опция автоформатирования включена, форматирование будет применяться автоматически при каждом сохранении.

### Команда форматирования

В контекстном меню файла можно вызвать команду:

- `Format PL+ code`

## Поддерживаемые расширения

- `.plp`
- `.plsql`
- `.pls`
- `.sql`

## Сборка из исходников

Если вы хотите открыть проект в Eclipse как плагин и собрать его самостоятельно:

1. Скачайте репозиторий с GitHub или клонируйте его:
   ```bash
   git clone https://github.com/<username>/<repository>.git
   ```
2. В Eclipse выберите:
   `File -> Import -> Existing Projects into Workspace`
3. Укажите папку проекта.
4. Убедитесь, что установлен `Eclipse Plug-in Development`.
5. Запустите проект как Eclipse Application для проверки.
6. При необходимости экспортируйте плагин через:
   `File -> Export -> Plug-in Development -> Deployable plug-ins and fragments`

## Структура проекта

```text
PL-PLUS-formatter-for-Eclipse/
├── .classpath
├── .project
├── build.properties
├── plugin.xml
├── META-INF/
│   └── MANIFEST.MF
├── src/
│   └── plplus/
│       └── formatter/
│           ├── Activator.java
│           ├── FormatHandler.java
│           ├── PlPlusAutoFormatter.java
│           ├── PlPlusEditor.java
│           ├── PlPlusFormatter.java
│           ├── PlPlusPreferencePage.java
│           ├── PlPlusPreferences.java
│           ├── PlPlusStartup.java
│           └── PreferenceInitializer.java
├── README.md
├── LICENSE
└── .gitignore
```

## Пример форматирования

### До

```plsql
BEGIN
IF x=1 THEN
SELECT * FROM t WHERE a=1;
END IF;
END;
```

### После

```plsql
BEGIN
  IF x = 1 THEN
    SELECT *
      FROM t
     WHERE a = 1;
  END IF;
END;
```

## Примечание

Это базовая реализация форматтера для PL/SQL (PL+), рассчитанная на простую и понятную работу в Eclipse. Для более сложного и строго структурированного форматирования можно расширить логику парсинга и правил форматирования по конкретному стандарту команды или проекта.

## Лицензия

Проект распространяется под лицензией MIT.
