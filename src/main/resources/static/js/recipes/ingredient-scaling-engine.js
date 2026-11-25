/**
 * Unified Ingredient Scaling, Pluralization, and Package Preview Engine
 * Shared by Recipe Viewer (view.js) and Shopping List (shopping-list.js).
 */
window.RecipeScalingEngine = (function () {

    function getMeasurementMatrix() {
        const metaEl = document.getElementById("recipe-page-meta") || document.getElementById("measurement-pool-meta");
        if (!metaEl || !metaEl.dataset.measurements) return [];
        try {
            return JSON.parse(metaEl.dataset.measurements);
        } catch (e) {
            console.error("Failed to parse measurement matrix:", e);
            return [];
        }
    }

    function formatToKitchenFraction(value) {
        let firstPart = Math.floor(value);
        let decimalPart = Math.round((value - firstPart) * 1000) / 1000;
        let fraction = "";

        if (decimalPart >= 0.08 && decimalPart <= 0.16) fraction = "1/8";
        else if (decimalPart > 0.16 && decimalPart <= 0.29) fraction = "1/4";
        else if (decimalPart > 0.29 && decimalPart <= 0.40) fraction = "1/3";
        else if (decimalPart > 0.40 && decimalPart <= 0.58) fraction = "1/2";
        else if (decimalPart > 0.58 && decimalPart <= 0.70) fraction = "2/3";
        else if (decimalPart > 0.70 && decimalPart <= 0.82) fraction = "3/4";
        else if (decimalPart > 0.82 && decimalPart <= 0.93) fraction = "7/8";
        else if (decimalPart > 0.93) { firstPart++; decimalPart = 0; }

        const output = (firstPart > 0 ? firstPart : "") + (fraction ? " " + fraction : "");
        return output.trim() || "0";
    }

    function resolveUnitAndQuantity(params) {
        const {
            baseTypeId,
            baseUnitEquivalent,
            originalUnitId,
            isPromotable,
            classification,
            totalScaledValue,
            absoluteBaseUnitsSum,
            nativeDatabaseUnitText,
            targetMetricFlag,
            poolMatrix
        } = params;

        const originalUnitMeta = !isNaN(originalUnitId)
            ? poolMatrix.find(function (m) { return m.measurementId === originalUnitId; })
            : null;

        let finalTargetUnitText = originalUnitMeta ? originalUnitMeta.unitName : nativeDatabaseUnitText;
        let finalDisplayQuantity = totalScaledValue;
        const hasValidMeta = !isNaN(baseTypeId) && !isNaN(baseUnitEquivalent);

        if (!hasValidMeta || (baseTypeId !== 1 && baseTypeId !== 2)) {
            return { unitText: finalTargetUnitText, quantity: finalDisplayQuantity, originalUnitMeta };
        }

        const effectiveTargetBaseTypeId = (targetMetricFlag && baseTypeId === 1 && classification === "DRY")
            ? 2
            : baseTypeId;

        let candidates = poolMatrix.filter(function (m) {
            return m.baseTypeId === effectiveTargetBaseTypeId && Boolean(m.isMetric) === targetMetricFlag;
        });

        if (effectiveTargetBaseTypeId === 1 && !targetMetricFlag && classification === "DRY") {
            candidates = candidates.filter(function (m) {
                const nameLower = m.unitName.toLowerCase();
                return nameLower.includes("teaspoon") ||
                    nameLower.includes("tablespoon") ||
                    nameLower.includes("cup") ||
                    nameLower.includes("pinch");
            });
        }

        candidates.sort(function (a, b) { return b.scalingRank - a.scalingRank; });

        const originalIsMetric = originalUnitMeta ? Boolean(originalUnitMeta.isMetric) : false;
        const isCrossSystemConversion = (originalIsMetric !== targetMetricFlag);

        if (isPromotable || isCrossSystemConversion) {
            const baseUnitFallback = candidates.find(function (m) { return m.scalingRank === 1; }) || candidates[candidates.length - 1];
            if (baseUnitFallback) {
                finalTargetUnitText = baseUnitFallback.unitName;
                finalDisplayQuantity = absoluteBaseUnitsSum / parseFloat(baseUnitFallback.baseUnitEquivalent);
            }

            let unitPromoted = false;

            for (let i = 0; i < candidates.length; i++) {
                const candidate = candidates[i];
                const equivValue = parseFloat(candidate.baseUnitEquivalent);
                const quantityInCandidateUnit = absoluteBaseUnitsSum / equivValue;
                const dynamicFloor = parseFloat(candidate.minPromotionFloor) || 0.85;

                const satisfiesThreshold = (dynamicFloor < 1.0 && dynamicFloor > 0.0 && dynamicFloor !== 0.85)
                    ? (quantityInCandidateUnit >= dynamicFloor)
                    : (absoluteBaseUnitsSum >= (equivValue * dynamicFloor));

                if (satisfiesThreshold && (candidate.isPromotable || candidate.measurementId === originalUnitId)) {
                    finalTargetUnitText = candidate.unitName;
                    finalDisplayQuantity = quantityInCandidateUnit;
                    unitPromoted = true;
                    break;
                }
            }

            if (!unitPromoted && !isCrossSystemConversion && originalUnitMeta) {
                finalTargetUnitText = originalUnitMeta.unitName;
                finalDisplayQuantity = totalScaledValue;
            }
        }

        return { unitText: finalTargetUnitText, quantity: finalDisplayQuantity, originalUnitMeta };
    }
    function updateAll(options = {}) {
        const scalingFactor = (typeof options.scalingFactor === "number" && options.scalingFactor > 0)
            ? options.scalingFactor
            : 1;
        const targetMetricFlag = (options.system === "metric");
        const poolMatrix = Array.isArray(options.matrix) ? options.matrix : getMeasurementMatrix();
        const root = options.root || document;

        root.querySelectorAll(".scalable-amount").forEach(function (amountEl) {
            const parentEl = amountEl.parentElement;
            const unitEl = parentEl ? parentEl.querySelector(".scalable-unit-name") : null;
            const ozPreviewEl = parentEl ? parentEl.querySelector(".oz-package-preview") : null;

            const baseAmount = parseFloat(amountEl.dataset.baseAmount);
            if (isNaN(baseAmount) || baseAmount <= 0) return;

            const baseTypeId = parseInt(amountEl.dataset.baseTypeId, 10);
            const baseUnitEquivalent = parseFloat(amountEl.dataset.baseUnitEquivalent);
            const originalUnitId = parseInt(amountEl.dataset.originalUnitId, 10);
            const isPromotable = amountEl.dataset.isPromotable === "true";
            const classification = (amountEl.dataset.ingredientClassification || "DRY").toUpperCase().trim();

            const totalScaledValue = baseAmount * scalingFactor;
            const hasValidMeta = !isNaN(baseTypeId) && !isNaN(baseUnitEquivalent);
            const absoluteBaseUnitsSum = hasValidMeta ? (totalScaledValue * baseUnitEquivalent) : 0;

            const nativeDatabaseUnitText = unitEl
                ? (unitEl.getAttribute("data-original-unit-name") || unitEl.textContent.trim() || "")
                : "";

            const resolved = resolveUnitAndQuantity({
                baseTypeId,
                baseUnitEquivalent,
                originalUnitId,
                isPromotable,
                classification,
                totalScaledValue,
                absoluteBaseUnitsSum,
                nativeDatabaseUnitText,
                targetMetricFlag,
                poolMatrix
            });

            // 1. Format numeric amount
            if (targetMetricFlag && hasValidMeta && (baseTypeId === 1 || baseTypeId === 2)) {
                const unitLower = resolved.unitText.toLowerCase();
                const isLargeMetricUnit = (unitLower === "kilogram" || unitLower === "liter");
                const precisionFactor = isLargeMetricUnit ? 100 : (resolved.quantity >= 10 ? 1 : 10);
                amountEl.textContent = String(Math.round(resolved.quantity * precisionFactor) / precisionFactor);
            } else {
                amountEl.textContent = formatToKitchenFraction(resolved.quantity);
            }

            // 2. Database-driven singular/plural unit resolution & "Whole" suppression
            if (unitEl) {
                let finalUnitTextOutput = resolved.unitText;
                const matchedMatrixUnit = (resolved.originalUnitMeta && resolved.originalUnitMeta.unitName === resolved.unitText)
                    ? resolved.originalUnitMeta
                    : poolMatrix.find(function (m) {
                        return m.unitName === resolved.unitText || m.pluralName === resolved.unitText;
                    });

                if (matchedMatrixUnit) {
                    finalUnitTextOutput = (resolved.quantity <= 1)
                        ? matchedMatrixUnit.unitName
                        : (matchedMatrixUnit.pluralName || matchedMatrixUnit.unitName);
                }

                if (!finalUnitTextOutput || finalUnitTextOutput.toLowerCase() === "whole") {
                    unitEl.textContent = "";
                } else {
                    unitEl.textContent = finalUnitTextOutput;
                }
            }

            // 3. Ounce / Fluid Ounce package preview
            if (ozPreviewEl) {
                if (hasValidMeta && (baseTypeId === 1 || baseTypeId === 2) && !targetMetricFlag) {
                    const isDryOrWeight = (baseTypeId === 1 && classification === "DRY") || baseTypeId === 2;
                    const divFactor = isDryOrWeight ? 28.349523 : 29.573530;
                    const suffix = isDryOrWeight ? " OZ" : " FL OZ";
                    const cleanOz = Math.round((absoluteBaseUnitsSum / divFactor) * 10) / 10;
                    ozPreviewEl.textContent = cleanOz > 0 ? (" (" + cleanOz + suffix + ")") : "";
                } else {
                    ozPreviewEl.textContent = "";
                }
            }
        });
    }

    return {
        getMeasurementMatrix: getMeasurementMatrix,
        formatToKitchenFraction: formatToKitchenFraction,
        updateAll: updateAll
    };
})();
