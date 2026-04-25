import pandas as pd
import numpy as np
import glob
import os
import sys

# --- MATEMATİK FONKSİYONLARI ---
def quaternion_to_euler(q0, q1, q2, q3):
    """Quaternion verisini Euler açılarına (Derece) çevirir."""
    RAD2DEG = 180.0 / np.pi
    sinr_cosp = 2 * (q0 * q1 + q2 * q3)
    cosr_cosp = 1 - 2 * (q1 * q1 + q2 * q2)
    roll = np.arctan2(sinr_cosp, cosr_cosp)

    sinp = 2 * (q0 * q2 - q3 * q1)
    pitch = np.where(np.abs(sinp) >= 1, np.sign(sinp) * np.pi / 2, np.arcsin(sinp))

    siny_cosp = 2 * (q0 * q3 + q1 * q2)
    cosy_cosp = 1 - 2 * (q2 * q2 + q3 * q3)
    yaw = np.arctan2(siny_cosp, cosy_cosp)
    return roll * RAD2DEG, pitch * RAD2DEG, yaw * RAD2DEG

def process_log(folder_path):
    # 1. DOSYALARI BUL
    patterns = {
        'attitude': '*vehicle_attitude_0.csv',
        'att_sp': '*vehicle_attitude_setpoint_0.csv',
        'rates': '*vehicle_angular_velocity_0.csv',
        'rates_sp': '*vehicle_rates_setpoint_0.csv',
        'control': '*vehicle_control_mode_0.csv'
    }
    
    files = {}
    for key, pattern in patterns.items():
        search_path = os.path.join(folder_path, pattern)
        found = glob.glob(search_path)
        if not found:
            files[key] = None
        else:
            files[key] = found[0]

    # Control file zorunlu (ARMED süresi için)
    if files['control'] is None:
        print(f"Error: Control file not found in {folder_path}")
        return None

    # 2. ARMED SÜRESİNİ BUL (Uçuşun başladığı ve bittiği an)
    try:
        control_df = pd.read_csv(files['control'])
        # flag_armed = 1 olduğu satırları al
        armed_data = control_df[control_df['flag_armed'] == 1]
        
        if armed_data.empty:
            print("Error: No ARMED state found (Vehicle never flew).")
            return None
            
        START_TIME = armed_data['timestamp'].min()
        END_TIME = armed_data['timestamp'].max()
    except Exception as e:
        print(f"Error reading control file: {e}")
        return None
    
    # 3. SENKRONİZASYON (10Hz = 100ms)
    STEP_US = 100000 
    common_time = np.arange(START_TIME, END_TIME, STEP_US)
    final_df = pd.DataFrame({'timestamp': common_time})
    
    # Yardımcı Fonksiyon: CSV'yi okur, 10Hz'e indirger ve ana tabloya ekler
    def resample_and_merge(fname, cols_to_read, new_names, convert_euler=False, is_setpoint=False):
        if fname is None: return
        try:
            df = pd.read_csv(fname)
            
            # Quaternion Dönüşümü (Eğer istenmişse)
            # Normal Attitude: q[0]..q[3]
            # Setpoint: q_d[0]..q_d[3]
            q_prefix = 'q_d' if is_setpoint else 'q'
            
            if convert_euler and f'{q_prefix}[0]' in df.columns:
                r, p, y = quaternion_to_euler(
                    df[f'{q_prefix}[0]'], 
                    df[f'{q_prefix}[1]'], 
                    df[f'{q_prefix}[2]'], 
                    df[f'{q_prefix}[3]']
                )
                # Geçici olarak dataframe'e ekle
                prefix = '_sp' if is_setpoint else ''
                df[f'roll{prefix}'] = r
                df[f'pitch{prefix}'] = p
                df[f'yaw{prefix}'] = y
            
            # İnterpolasyon (Zaman eksenini eşleme)
            for i, col in enumerate(cols_to_read):
                # Eğer bu sütun hesaplanmış euler ise veya orijinal dosyada varsa
                if col in df.columns:
                    f_interp = np.interp(common_time, df['timestamp'], df[col])
                    final_df[new_names[i]] = f_interp
                    
        except Exception as e:
            print(f"Warning processing {fname}: {e}")

    # --- VERİ İŞLEME AŞAMALARI ---

    # 1. Attitude (Gerçekleşen Açılar)
    if files['attitude']:
        resample_and_merge(files['attitude'], 
                           ['roll', 'pitch', 'yaw'], 
                           ['roll', 'pitch', 'yaw'], 
                           convert_euler=True, is_setpoint=False)
    
    # 2. Attitude Setpoint (Hedef Açılar)
    if files['att_sp']:
        resample_and_merge(files['att_sp'], 
                           ['roll_sp', 'pitch_sp', 'yaw_sp'], 
                           ['roll_sp', 'pitch_sp', 'yaw_sp'], 
                           convert_euler=True, is_setpoint=True)

    # 3. Rates (Gerçekleşen Açısal Hızlar)
    # Genellikle radyan/sn gelir, derece/sn'ye çevireceğiz.
    if files['rates']:
        try:
            df_rate = pd.read_csv(files['rates'])
            RAD2DEG = 180.0 / np.pi
            
            # xyz[0]=RollRate, xyz[1]=PitchRate, xyz[2]=YawRate
            if 'xyz[0]' in df_rate.columns:
                df_rate['roll_rate'] = df_rate['xyz[0]'] * RAD2DEG
                df_rate['pitch_rate'] = df_rate['xyz[1]'] * RAD2DEG
                df_rate['yaw_rate'] = df_rate['xyz[2]'] * RAD2DEG
                
                # Merge
                final_df['roll_rate'] = np.interp(common_time, df_rate['timestamp'], df_rate['roll_rate'])
                final_df['pitch_rate'] = np.interp(common_time, df_rate['timestamp'], df_rate['pitch_rate'])
                final_df['yaw_rate'] = np.interp(common_time, df_rate['timestamp'], df_rate['yaw_rate'])
        except Exception as e:
            print(f"Warning processing rates: {e}")

    # 4. Rates Setpoint (Hedef Açısal Hızlar)
    if files['rates_sp']:
        try:
            df_rsp = pd.read_csv(files['rates_sp'])
            RAD2DEG = 180.0 / np.pi
            
            # Bu dosyada sütun adları genellikle 'roll', 'pitch', 'yaw' şeklindedir ama aslında rate'dir.
            if 'roll' in df_rsp.columns:
                df_rsp['roll_rate_sp'] = df_rsp['roll'] * RAD2DEG
                df_rsp['pitch_rate_sp'] = df_rsp['pitch'] * RAD2DEG
                df_rsp['yaw_rate_sp'] = df_rsp['yaw'] * RAD2DEG
                
                final_df['roll_rate_sp'] = np.interp(common_time, df_rsp['timestamp'], df_rsp['roll_rate_sp'])
                final_df['pitch_rate_sp'] = np.interp(common_time, df_rsp['timestamp'], df_rsp['pitch_rate_sp'])
                final_df['yaw_rate_sp'] = np.interp(common_time, df_rsp['timestamp'], df_rsp['yaw_rate_sp'])
        except Exception as e:
             print(f"Warning processing rates setpoint: {e}")

    
    # 4. KAYDETME
    # Zamanı okunabilir saniyeye çevir
    final_df['Time_s'] = (final_df['timestamp'] - START_TIME) / 1e6
    
    # Sütun Sıralaması (Okunabilirlik için)
    # Önce zaman, sonra setpoint-actual ikilileri
    desired_order = ['Time_s', 
                     'roll_sp', 'roll', 'roll_rate_sp', 'roll_rate', 
                     'pitch_sp', 'pitch', 'pitch_rate_sp', 'pitch_rate', 
                     'yaw_sp', 'yaw', 'yaw_rate_sp', 'yaw_rate']
    
    # Sadece mevcut olan sütunları al
    final_cols = [c for c in desired_order if c in final_df.columns]
    
    output_path = os.path.join(folder_path, "LLM_OPTIMIZED_DATA.csv")
    
    # Virgülden sonra 2 hane yeterli (Token tasarrufu)
    final_df[final_cols].round(2).to_csv(output_path, index=False)
    
    return output_path

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python flight_optimizer.py <folder_path>")
        sys.exit(1)
        
    folder = sys.argv[1]
    result = process_log(folder)
    if result:
        print(result)
    else:
        sys.exit(1)