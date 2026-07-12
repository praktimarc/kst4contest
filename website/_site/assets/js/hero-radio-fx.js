(() => {
    "use strict";

    const canvas = document.querySelector("[data-hero-radio-fx]");

    if (!(canvas instanceof HTMLCanvasElement)) {
        return;
    }

    const context = canvas.getContext("2d", {
        alpha: true,
        desynchronized: true
    });

    if (!context) {
        return;
    }

    const hero = canvas.closest(".hero");

    if (!(hero instanceof HTMLElement)) {
        return;
    }

    const reducedMotion = window.matchMedia(
        "(prefers-reduced-motion: reduce)"
    );

    const coarsePointer = window.matchMedia(
        "(pointer: coarse)"
    );

    const state = {
        width: 0,
        height: 0,
        dpr: 1,
        running: false,
        visible: true,
        lastFrame: 0,
        spawnAccumulator: 0,
        beamAccumulator: 0,
        animationFrameId: 0,
        pointer: {
            x: 0,
            y: 0,
            active: false
        },
        planes: [],
        beams: [],
        reflections: []
    };

    const clamp = (value, minimum, maximum) =>
        Math.max(minimum, Math.min(maximum, value));

    const randomBetween = (minimum, maximum) =>
        minimum + Math.random() * (maximum - minimum);

    function getCssColor(name, fallback) {
        const value = getComputedStyle(document.documentElement)
            .getPropertyValue(name)
            .trim();

        return value || fallback;
    }

    function getPalette() {
        return {
            plane: getCssColor("--fx-plane", "#79ff73"),
            planeMuted: getCssColor("--fx-plane-muted", "rgba(121,255,115,.42)"),
            beam: getCssColor("--fx-beam", "#52ff65"),
            beamGlow: getCssColor("--fx-beam-glow", "rgba(82,255,101,.28)"),
            reflection: getCssColor("--fx-reflection", "#c8ffbf"),
            trail: getCssColor("--fx-trail", "rgba(121,255,115,.16)"),
            grid: getCssColor("--fx-grid", "rgba(121,255,115,.055)")
        };
    }

    function resizeCanvas() {
        const bounds = hero.getBoundingClientRect();

        state.width = Math.max(1, Math.round(bounds.width));
        state.height = Math.max(1, Math.round(bounds.height));
        state.dpr = Math.min(window.devicePixelRatio || 1, 1.5);

        canvas.width = Math.round(state.width * state.dpr);
        canvas.height = Math.round(state.height * state.dpr);

        canvas.style.width = `${state.width}px`;
        canvas.style.height = `${state.height}px`;

        context.setTransform(
            state.dpr,
            0,
            0,
            state.dpr,
            0,
            0
        );

        if (!state.pointer.active) {
            state.pointer.x = state.width * 0.54;
            state.pointer.y = state.height * 0.42;
        }
    }

    function getPlaneLimit() {
        if (coarsePointer.matches) {
            return 5;
        }

        if (state.width < 900) {
            return 7;
        }

        return 12;
    }

    function createPlane() {
        const margin = 55;
        const edge = Math.floor(Math.random() * 4);

        let x;
        let y;

        switch (edge) {
            case 0:
                x = randomBetween(0, state.width);
                y = -margin;
                break;

            case 1:
                x = state.width + margin;
                y = randomBetween(0, state.height);
                break;

            case 2:
                x = randomBetween(0, state.width);
                y = state.height + margin;
                break;

            default:
                x = -margin;
                y = randomBetween(0, state.height);
                break;
        }

        const targetX = state.pointer.active
            ? state.pointer.x
            : state.width * randomBetween(0.38, 0.72);

        const targetY = state.pointer.active
            ? state.pointer.y
            : state.height * randomBetween(0.2, 0.72);

        const angle = Math.atan2(targetY - y, targetX - x);
        const speed = randomBetween(18, 34);

        return {
            x,
            y,
            previousX: x,
            previousY: y,
            velocityX: Math.cos(angle) * speed,
            velocityY: Math.sin(angle) * speed,
            angle,
            size: randomBetween(5.5, 9),
            age: 0,
            maximumAge: randomBetween(12, 20),
            steering: randomBetween(0.65, 1.2),
            wobble: randomBetween(0, Math.PI * 2),
            wobbleSpeed: randomBetween(0.7, 1.4),
            hit: 0
        };
    }

    function updatePlane(plane, deltaSeconds) {
        plane.age += deltaSeconds;
        plane.wobble += deltaSeconds * plane.wobbleSpeed;

        plane.previousX = plane.x;
        plane.previousY = plane.y;

        const targetX = state.pointer.active
            ? state.pointer.x
            : state.width * 0.56;

        const targetY = state.pointer.active
            ? state.pointer.y
            : state.height * 0.44;

        const desiredAngle = Math.atan2(
            targetY - plane.y,
            targetX - plane.x
        );

        let angleDifference = desiredAngle - plane.angle;

        while (angleDifference > Math.PI) {
            angleDifference -= Math.PI * 2;
        }

        while (angleDifference < -Math.PI) {
            angleDifference += Math.PI * 2;
        }

        plane.angle += angleDifference * plane.steering * deltaSeconds;

        const speed = Math.hypot(
            plane.velocityX,
            plane.velocityY
        );

        const wobble = Math.sin(plane.wobble) * 0.14;

        plane.velocityX =
            Math.cos(plane.angle + wobble) * speed;

        plane.velocityY =
            Math.sin(plane.angle + wobble) * speed;

        plane.x += plane.velocityX * deltaSeconds;
        plane.y += plane.velocityY * deltaSeconds;

        plane.hit = Math.max(0, plane.hit - deltaSeconds * 1.8);
    }

    function isPlaneExpired(plane) {
        const margin = 180;

        return (
            plane.age > plane.maximumAge ||
            plane.x < -margin ||
            plane.x > state.width + margin ||
            plane.y < -margin ||
            plane.y > state.height + margin
        );
    }

    function chooseBeamTarget() {
        if (state.planes.length === 0) {
            return null;
        }

        const targetX = state.pointer.active
            ? state.pointer.x
            : state.width * 0.56;

        const targetY = state.pointer.active
            ? state.pointer.y
            : state.height * 0.44;

        const candidates = state.planes
            .map((plane) => ({
                plane,
                distance: Math.hypot(
                    plane.x - targetX,
                    plane.y - targetY
                )
            }))
            .filter((entry) => entry.distance < 260)
            .sort((a, b) => a.distance - b.distance);

        if (candidates.length > 0) {
            return candidates[0].plane;
        }

        return state.planes[
            Math.floor(Math.random() * state.planes.length)
            ];
    }

    function createBeam(target) {
        const leftStation = {
            x: state.width * 0.12,
            y: state.height * 0.82
        };

        const rightStation = {
            x: state.width * 0.88,
            y: state.height * 0.78
        };

        const source =
            Math.abs(target.x - leftStation.x) <
            Math.abs(target.x - rightStation.x)
                ? leftStation
                : rightStation;

        state.beams.push({
            startX: source.x,
            startY: source.y,
            endX: target.x,
            endY: target.y,
            age: 0,
            maximumAge: 0.62
        });

        target.hit = 1;

        state.reflections.push({
            x: target.x,
            y: target.y,
            age: 0,
            maximumAge: 0.9,
            radius: randomBetween(8, 15)
        });
    }

    function updateEffects(deltaSeconds) {
        for (const beam of state.beams) {
            beam.age += deltaSeconds;
        }

        state.beams = state.beams.filter(
            (beam) => beam.age < beam.maximumAge
        );

        for (const reflection of state.reflections) {
            reflection.age += deltaSeconds;
            reflection.radius += deltaSeconds * 30;
        }

        state.reflections = state.reflections.filter(
            (reflection) =>
                reflection.age < reflection.maximumAge
        );
    }

    function drawGrid(palette) {
        context.save();

        context.strokeStyle = palette.grid;
        context.lineWidth = 1;

        const spacing = 54;

        for (
            let x = spacing;
            x < state.width;
            x += spacing
        ) {
            context.beginPath();
            context.moveTo(x, 0);
            context.lineTo(x, state.height);
            context.stroke();
        }

        for (
            let y = spacing;
            y < state.height;
            y += spacing
        ) {
            context.beginPath();
            context.moveTo(0, y);
            context.lineTo(state.width, y);
            context.stroke();
        }

        context.restore();
    }

    function drawRadarTarget(palette) {
        const x = state.pointer.active
            ? state.pointer.x
            : state.width * 0.56;

        const y = state.pointer.active
            ? state.pointer.y
            : state.height * 0.44;

        context.save();

        context.strokeStyle = palette.planeMuted;
        context.lineWidth = 1;

        for (const radius of [24, 48, 82]) {
            context.globalAlpha = 0.24 - radius / 500;

            context.beginPath();
            context.arc(x, y, radius, 0, Math.PI * 2);
            context.stroke();
        }

        context.globalAlpha = 0.2;

        context.beginPath();
        context.moveTo(x - 100, y);
        context.lineTo(x + 100, y);
        context.moveTo(x, y - 100);
        context.lineTo(x, y + 100);
        context.stroke();

        context.restore();
    }

    function drawPlane(plane, palette) {
        context.save();

        context.translate(plane.x, plane.y);
        context.rotate(plane.angle);

        const alpha = clamp(
            Math.min(
                plane.age * 1.8,
                (plane.maximumAge - plane.age) * 1.8
            ),
            0,
            1
        );

        context.globalAlpha = alpha;

        context.strokeStyle = palette.trail;
        context.lineWidth = 1;

        context.beginPath();
        context.moveTo(-plane.size * 4.5, 0);
        context.lineTo(-plane.size * 1.4, 0);
        context.stroke();

        context.fillStyle =
            plane.hit > 0
                ? palette.reflection
                : palette.plane;

        context.shadowColor =
            plane.hit > 0
                ? palette.reflection
                : palette.beamGlow;

        context.shadowBlur =
            plane.hit > 0
                ? 16
                : 5;

        const size = plane.size;

        context.beginPath();

        context.moveTo(size * 1.65, 0);
        context.lineTo(-size * 0.7, size * 0.34);
        context.lineTo(-size * 0.15, size * 1.05);
        context.lineTo(-size * 0.65, size * 1.08);
        context.lineTo(-size * 1.35, size * 0.3);
        context.lineTo(-size * 1.7, size * 0.28);
        context.lineTo(-size * 1.12, 0);
        context.lineTo(-size * 1.7, -size * 0.28);
        context.lineTo(-size * 1.35, -size * 0.3);
        context.lineTo(-size * 0.65, -size * 1.08);
        context.lineTo(-size * 0.15, -size * 1.05);
        context.lineTo(-size * 0.7, -size * 0.34);

        context.closePath();
        context.fill();

        context.restore();
    }

    function drawBeam(beam, palette) {
        const progress = beam.age / beam.maximumAge;
        const alpha = Math.sin(progress * Math.PI);

        context.save();

        context.globalAlpha = alpha;
        context.strokeStyle = palette.beamGlow;
        context.lineWidth = 6;
        context.shadowColor = palette.beam;
        context.shadowBlur = 15;

        context.beginPath();
        context.moveTo(beam.startX, beam.startY);
        context.lineTo(beam.endX, beam.endY);
        context.stroke();

        context.strokeStyle = palette.beam;
        context.lineWidth = 1.4;
        context.shadowBlur = 5;

        context.beginPath();
        context.moveTo(beam.startX, beam.startY);
        context.lineTo(beam.endX, beam.endY);
        context.stroke();

        context.restore();
    }

    function drawReflection(reflection, palette) {
        const progress =
            reflection.age / reflection.maximumAge;

        context.save();

        context.globalAlpha = 1 - progress;
        context.strokeStyle = palette.reflection;
        context.lineWidth = 1.5;
        context.shadowColor = palette.beam;
        context.shadowBlur = 12;

        context.beginPath();
        context.arc(
            reflection.x,
            reflection.y,
            reflection.radius,
            0,
            Math.PI * 2
        );
        context.stroke();

        context.restore();
    }

    function drawStaticScene() {
        const palette = getPalette();

        context.clearRect(
            0,
            0,
            state.width,
            state.height
        );

        drawGrid(palette);
        drawRadarTarget(palette);
    }

    function drawScene() {
        const palette = getPalette();

        context.clearRect(
            0,
            0,
            state.width,
            state.height
        );

        drawGrid(palette);
        drawRadarTarget(palette);

        for (const plane of state.planes) {
            drawPlane(plane, palette);
        }

        for (const beam of state.beams) {
            drawBeam(beam, palette);
        }

        for (const reflection of state.reflections) {
            drawReflection(reflection, palette);
        }
    }

    function animate(timestamp) {
        if (!state.running) {
            return;
        }

        const deltaSeconds = clamp(
            (timestamp - state.lastFrame) / 1000 || 0,
            0,
            0.05
        );

        state.lastFrame = timestamp;

        if (!state.visible || document.hidden) {
            state.animationFrameId =
                requestAnimationFrame(animate);

            return;
        }

        state.spawnAccumulator += deltaSeconds;
        state.beamAccumulator += deltaSeconds;

        const planeLimit = getPlaneLimit();
        const spawnInterval =
            coarsePointer.matches ? 2.2 : 1.15;

        if (
            state.spawnAccumulator >= spawnInterval &&
            state.planes.length < planeLimit
        ) {
            state.spawnAccumulator = 0;
            state.planes.push(createPlane());
        }

        const beamInterval =
            coarsePointer.matches ? 3.3 : 1.8;

        if (
            state.beamAccumulator >= beamInterval &&
            state.planes.length > 0
        ) {
            state.beamAccumulator = 0;

            const target = chooseBeamTarget();

            if (target) {
                createBeam(target);
            }
        }

        for (const plane of state.planes) {
            updatePlane(plane, deltaSeconds);
        }

        state.planes = state.planes.filter(
            (plane) => !isPlaneExpired(plane)
        );

        updateEffects(deltaSeconds);
        drawScene();

        state.animationFrameId =
            requestAnimationFrame(animate);
    }

    function start() {
        cancelAnimationFrame(state.animationFrameId);

        resizeCanvas();

        if (reducedMotion.matches) {
            state.running = false;
            drawStaticScene();
            return;
        }

        state.running = true;
        state.lastFrame = performance.now();

        if (state.planes.length === 0) {
            const initialPlanes =
                coarsePointer.matches ? 2 : 5;

            for (let index = 0; index < initialPlanes; index++) {
                const plane = createPlane();
                plane.age = Math.random() * 2;
                state.planes.push(plane);
            }
        }

        state.animationFrameId =
            requestAnimationFrame(animate);
    }

    function updatePointer(event) {
        const bounds = hero.getBoundingClientRect();

        state.pointer.x = clamp(
            event.clientX - bounds.left,
            0,
            bounds.width
        );

        state.pointer.y = clamp(
            event.clientY - bounds.top,
            0,
            bounds.height
        );

        state.pointer.active = true;
    }

    hero.addEventListener("pointermove", updatePointer, {
        passive: true
    });

    hero.addEventListener("pointerleave", () => {
        state.pointer.active = false;
    });

    window.addEventListener("resize", resizeCanvas, {
        passive: true
    });

    document.addEventListener("visibilitychange", () => {
        state.lastFrame = performance.now();
    });

    reducedMotion.addEventListener("change", start);

    const resizeObserver = new ResizeObserver(() => {
        resizeCanvas();

        if (reducedMotion.matches) {
            drawStaticScene();
        }
    });

    resizeObserver.observe(hero);

    const intersectionObserver = new IntersectionObserver(
        (entries) => {
            state.visible =
                entries[0]?.isIntersecting ?? true;

            state.lastFrame = performance.now();
        },
        {
            threshold: 0.05
        }
    );

    intersectionObserver.observe(hero);

    start();
})();