package com.academconnect.factories;

import com.academconnect.domain.ModoEvaluacion;
import com.academconnect.domain.TipoTrabajo;
import com.academconnect.domain.TipoTrabajoConfig;

public class TipoTrabajoConfigFactory {

    public static TipoTrabajoConfig createConfig(TipoTrabajo tipo, ModoEvaluacion modo) {
        TipoTrabajoConfig c = new TipoTrabajoConfig();
        c.setTipo(tipo);
        c.setModoEvaluacion(modo);
        c.setEvaluadoresDefault(3);
        return c;
    }
}
