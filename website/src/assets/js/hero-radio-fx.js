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

    function getRadioStations() {
        const centerX = state.width * 0.54;
        const centerY = state.height * 0.43;

        const left = {
            x: state.width * 0.075,
            y: state.height * 0.82,
            label: "JN49EM"
        };

        const right = {
            x: state.width * 0.925,
            y: state.height * 0.80,
            label: "JO66MD"
        };

        left.angle = Math.atan2(
            centerY - left.y,
            centerX - left.x
        );

        right.angle = Math.atan2(
            centerY - right.y,
            centerX - right.x
        );

        return [left, right];
    }

    function createBeam(target) {function createBeam(target) {
        const stations = getRadioStations();

        const source = stations
            .map((station) => ({
                station,
                distance: Math.hypot(
                    target.x - station.x,
                    target.y - station.y
                )
            }))
            .sort((a, b) => a.distance - b.distance)[0].station;

        const beamAngle = Math.atan2(
            target.y - source.y,
            target.x - source.x
        );

        /*
         * Der Strahl beginnt nicht im Antennenmast,
         * sondern etwas vor der Yagi.
         */
        const antennaTipDistance = 45;

        const startX =
            source.x +
            Math.cos(beamAngle) * antennaTipDistance;

        const startY =
            source.y +
            Math.sin(beamAngle) * antennaTipDistance;

        state.beams.push({
            startX,
            startY,
            endX: target.x,
            endY: target.y,
            age: 0,
            maximumAge: 0.72
        });

        target.hit = 1;

        const probabilities = [50, 75, 100];
        const probability =
            probabilities[
                Math.floor(Math.random() * probabilities.length)
                ];

        state.reflections.push({
            x: target.x,
            y: target.y,
            age: 0,
            maximumAge: 1.25,
            radius: randomBetween(8, 13),
            probability
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

        function drawYagi(station, palette) {
            context.save();

            context.translate(station.x, station.y);
            context.rotate(station.angle);

            context.strokeStyle = palette.plane;
            context.fillStyle = palette.plane;
            context.lineCap = "round";
            context.lineWidth = 1.5;

            context.shadowColor = palette.beamGlow;
            context.shadowBlur = 9;

            /*
             * Mast
             */
            context.beginPath();
            context.moveTo(-30, 26);
            context.lineTo(-30, -5);
            context.stroke();

            /*
             * Ausleger / Boom
             */
            context.beginPath();
            context.moveTo(-32, 0);
            context.lineTo(43, 0);
            context.stroke();

            /*
             * Reflektor
             */
            context.beginPath();
            context.moveTo(-27, -17);
            context.lineTo(-27, 17);
            context.stroke();

            /*
             * Dipol
             */
            context.beginPath();
            context.moveTo(-12, -14);
            context.lineTo(-12, 14);
            context.stroke();

            /*
             * Direktoren
             */
            const directors = [
                { x: 2, length: 22 },
                { x: 14, length: 19 },
                { x: 25, length: 16 },
                { x: 35, length: 13 }
            ];

            for (const director of directors) {
                context.beginPath();
                context.moveTo(
                    director.x,
                    -director.length / 2
                );
                context.lineTo(
                    director.x,
                    director.length / 2
                );
                context.stroke();
            }

            /*
             * Einspeisepunkt
             */
            context.beginPath();
            context.arc(-12, 0, 3.2, 0, Math.PI * 2);
            context.fill();

            context.restore();

            /*
             * Locator bleibt waagerecht lesbar.
             */
            context.save();

            context.font =
                '700 11px Inter, system-ui, sans-serif';

            const labelWidth =
                context.measureText(station.label).width + 14;

            const labelX = station.x - labelWidth / 2;
            const labelY = station.y + 32;

            context.fillStyle = "rgba(6, 18, 10, 0.86)";
            context.strokeStyle = palette.planeMuted;
            context.lineWidth = 1;

            context.beginPath();
            context.roundRect(
                labelX,
                labelY,
                labelWidth,
                23,
                6
            );
            context.fill();
            context.stroke();

            context.fillStyle = palette.plane;
            context.textAlign = "center";
            context.textBaseline = "middle";

            context.fillText(
                station.label,
                station.x,
                labelY + 11.5
            );

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
        context.lineWidth = 8;
        context.shadowColor = palette.beam;
        context.shadowBlur = 15;

        context.setLineDash([10, 6]);
        context.lineDashOffset = -progress * 36;

        context.beginPath();
        context.moveTo(beam.startX, beam.startY);
        context.lineTo(beam.endX, beam.endY);
        context.stroke();

        context.strokeStyle = palette.beam;
        context.lineWidth = 1.8;
        context.shadowBlur = 5;

        context.beginPath();
        context.moveTo(beam.startX, beam.startY);
        context.lineTo(beam.endX, beam.endY);
        context.stroke();

        context.setLineDash([]);

        context.restore();
    }

        function drawReflection(reflection, palette) {
            const progress =
                reflection.age / reflection.maximumAge;

            const alpha = 1 - progress;

            context.save();

            context.translate(
                reflection.x,
                reflection.y
            );

            context.globalAlpha = alpha;

            /*
             * Äußerer Reflexionsring
             */
            context.strokeStyle = palette.reflection;
            context.lineWidth = 1.6;
            context.shadowColor = palette.beam;
            context.shadowBlur = 18;

            context.beginPath();
            context.arc(
                0,
                0,
                reflection.radius,
                0,
                Math.PI * 2
            );
            context.stroke();

            /*
             * Zweiter Ring
             */
            context.globalAlpha = alpha * 0.55;

            context.beginPath();
            context.arc(
                0,
                0,
                reflection.radius * 1.65,
                0,
                Math.PI * 2
            );
            context.stroke();

            /*
             * Reflexionskreuz
             */
            context.globalAlpha = alpha * 0.8;
            context.lineWidth = 1;

            const crossSize =
                reflection.radius * 1.25;

            context.beginPath();
            context.moveTo(-crossSize, 0);
            context.lineTo(crossSize, 0);
            context.moveTo(0, -crossSize);
            context.lineTo(0, crossSize);
            context.stroke();

            /*
             * Heller Reflexionspunkt
             */
            context.globalAlpha = alpha;
            context.fillStyle = palette.reflection;
            context.shadowBlur = 24;

            context.beginPath();
            context.arc(0, 0, 3.8, 0, Math.PI * 2);
            context.fill();

            context.restore();

            /*
             * AP-Prozentwert
             */
            context.save();

            const label = `${reflection.probability}%`;

            context.font =
                '800 12px Inter, system-ui, sans-serif';

            const textWidth =
                context.measureText(label).width;

            const boxWidth = textWidth + 16;
            const boxHeight = 25;

            const boxX =
                reflection.x + reflection.radius + 12;

            const boxY =
                reflection.y - reflection.radius - 17;

            context.globalAlpha =
                clamp(alpha * 1.25, 0, 1);

            context.fillStyle = "rgba(6, 18, 10, 0.91)";
            context.strokeStyle = palette.plane;
            context.lineWidth = 1;

            context.shadowColor = palette.beamGlow;
            context.shadowBlur = 10;

            context.beginPath();
            context.roundRect(
                boxX,
                boxY,
                boxWidth,
                boxHeight,
                6
            );
            context.fill();
            context.stroke();

            context.shadowBlur = 0;
            context.fillStyle = palette.reflection;
            context.textAlign = "center";
            context.textBaseline = "middle";

            context.fillText(
                label,
                boxX + boxWidth / 2,
                boxY + boxHeight / 2
            );

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

        for (const station of getRadioStations()) {
            drawYagi(station, palette);
        }
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

        for (const station of getRadioStations()) {
            drawYagi(station, palette);
        }

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