---
name: commit
description: Crea commits de git siguiendo la convención del proyecto `feat(scope): descripción breve`. Usar cuando el usuario pida commitear, hacer un commit, guardar los cambios en git o armar el mensaje de commit.
---

# Commit con la convención del proyecto

## Formato

```
<tipo>(<scope>): <descripción breve>

<cuerpo opcional>
```

- **tipo**: `feat` por defecto (funcionalidad nueva o cambio visible). Usar otro solo si encaja claramente:
  - `fix`: corrige un bug
  - `refactor`: reestructura sin cambiar comportamiento
  - `style`: formato, sin cambios de lógica
  - `test`: agrega o corrige tests
  - `docs`: documentación, `CLAUDE.md`, skills
  - `chore`: build, dependencias, gradle, configuración
- **scope**: la parte de la app afectada, en kebab-case y minúscula. Preferir scopes que ya existan en el historial (`git log --oneline`). Ejemplos: `list-screen`, `strings-xml`, `base`, `reminder-edit`, `places`, `room`, `navigation`, `theme`, `notifications`, `geofence`.
- **descripción breve**:
  - En español, en minúscula, sin punto final.
  - En presente e impersonal: "agrega selector de pestañas", no "agregué" ni "Added".
  - Máximo ~72 caracteres en toda la primera línea.
  - Dice *qué* cambia, no *cómo*.
- **cuerpo**: solo si hace falta explicar el *por qué* o si el commit toca varias cosas. Dejar una línea en blanco después del título.

Ejemplos buenos:
```
feat(list-screen): agrega selector de pendientes y hechos
feat(room): agrega entidad y dao de recordatorios
fix(reminder-row): corrige miniatura deformada con textos largos
chore(gradle): agrega dependencias de room y ksp
```

## Pasos

1. Ver el estado con `git status` y `git diff` (más `git diff --staged` si ya hay algo en stage).
2. Si los cambios mezclan cosas no relacionadas (por ejemplo, una pantalla nueva y un cambio de gradle), proponer separarlos en varios commits, uno por tema.
3. Revisar que no se cuele nada que no corresponda: `local.properties`, `build/`, `.gradle/`, `.idea/workspace.xml`, `.claude/settings.local.json`, claves o secretos. Si aparece algo así, avisar y no incluirlo.
4. Agregar los archivos **por nombre** (`git add ruta/archivo.kt ...`), no con `git add -A` ni `git add .`.
5. Redactar el mensaje con el formato de arriba y commitear. Para mensajes multilínea, usar un heredoc:
   ```bash
   git commit -m "$(cat <<'EOF'
   feat(scope): descripción breve

   Cuerpo opcional.
   EOF
   )"
   ```
6. Mostrar el resultado con `git log --oneline -3`.

## No hacer
- No hacer `git push` salvo que el usuario lo pida explícitamente.
- No hacer `--amend`, `rebase` ni `reset` sin pedir confirmación.
- No usar `--no-verify`.
- No commitear directamente sobre `main` si el usuario está trabajando en una feature grande: sugerir una rama `feat/<scope>`.
